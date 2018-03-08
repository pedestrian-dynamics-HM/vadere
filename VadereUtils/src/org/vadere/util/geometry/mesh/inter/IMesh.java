package org.vadere.util.geometry.mesh.inter;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PMesh;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.iterators.AdjacentVertexIterator;
import org.vadere.util.geometry.mesh.iterators.EdgeIterator;
import org.vadere.util.geometry.mesh.iterators.AdjacentFaceIterator;
import org.vadere.util.geometry.mesh.iterators.EdgeOfVertexIterator;
import org.vadere.util.geometry.mesh.iterators.IncidentEdgeIterator;
import org.vadere.util.geometry.mesh.iterators.PointIterator;
import org.vadere.util.geometry.mesh.iterators.SurroundingFaceIterator;
import org.vadere.util.geometry.mesh.iterators.VertexIterator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.triangulation.IPointConstructor;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A IMesh is a undirected planar graph. It uses the half-edge data structure to store all information.
 * Different implementations are possible. One can use a doubled-linked-list data structure or a array/list
 * based data structure. It should be impossible to create faces, edges, and vertices of the mesh without using
 * the mesh i.e. IMesh is a factory for faces, edges and vertices.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IMesh<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F>, Cloneable {
	/**
	 * construct a new empty mesh.
	 * @return
	 */
	IMesh<P, V, E, F> construct();

	E getNext(@NotNull E halfEdge);
	E getPrev(@NotNull E halfEdge);
	E getTwin(@NotNull E halfEdge);
	F getFace(@NotNull E halfEdge);
	IPointConstructor<P> getPointConstructor();

	default F getNonBoundaryFace(@NotNull E halfEdge) {
        if(!isBoundary(halfEdge)) {
            return getFace(halfEdge);
        }
        else {
            return getFace(getTwin(halfEdge));
        }
    }

	default VLine toLine(@NotNull E halfEdge) {
		return new VLine(new VPoint(getVertex(getPrev(halfEdge))), new VPoint(getVertex(halfEdge)));
	}

	default VPoint toPoint(@NotNull V vertex) {
		return new VPoint(new VPoint(vertex));
	}

	default VPoint toPoint(@NotNull E edge) {
		return toPoint(getVertex(edge));
	}

	E getEdge(@NotNull V vertex);

	E getEdge(@NotNull F face);

	P getPoint(@NotNull E halfEdge);

	V getVertex(@NotNull E halfEdge);

	// TODO: this is for the delaunay-hierarchy only!
	V getDown(@NotNull V vertex);

	// TODO: this is for the delaunay-hierarchy only!
	void setDown(@NotNull V up, @NotNull V down);

	P getPoint(@NotNull V vertex);

	default F getTwinFace(@NotNull E halfEdge) {
		return getFace(getTwin(halfEdge));
	}
	F getFace();


	default F getNonBoundaryFace(@NotNull V vertex) {
		return getNonBoundaryFace(getEdge(vertex));
	}

	default F getFace(@NotNull V vertex) {
		return getFace(getEdge(vertex));
	}

	/**
	 * Returns true if the face is the boundary
	 *
	 * @param face
	 * @return
	 */
	boolean isBoundary(@NotNull F face);

	default boolean isBorder(@NotNull F face) {
		return isBoundary(face) && !isHole(face);
	}

	boolean isHole(@NotNull F face);

	/**
	 * Returns true if the vertex is a boundary vertex.
	 *
	 * @param vertex
	 * @return
	 */
	default boolean isAtBoundary(@NotNull V vertex) {
		return streamEdges(vertex).anyMatch(e -> isAtBoundary(e));
	}

	default boolean isAtBorder(@NotNull V vertex) {
		return streamEdges(vertex).anyMatch(e -> isAtBorder(e));
	}

	default boolean isAtBorder(@NotNull F face) {
		return streamEdges(face).anyMatch(e -> isAtBorder(e));
	}

	default boolean isAtBorder(@NotNull E edge) {
		return isBorder(edge) || isBorder(getTwin(edge));
	}

	default boolean isAtBoundary(@NotNull E edge) {
		return isBoundary(edge) || isBoundary(getTwin(edge));
	}

	default boolean isNeighbourBorder(@NotNull F face){
		for(F neighbourFace : getFaceIt(face)) {
			if(isBorder(neighbourFace)) {
				return true;
			}
		}

		return false;
	}

	default boolean isNeighbourBoundary(@NotNull F face){
		for(F neighbourFace : getFaceIt(face)) {
			if(isBoundary(neighbourFace)) {
				return true;
			}
		}

		return false;
	}

	default boolean isNeighbourHole(@NotNull F face){
		for(F neighbourFace : getFaceIt(face)) {
			if(isHole(neighbourFace)) {
				return true;
			}
		}

		return false;
	}

	default Optional<E> getLinkToBoundary(@NotNull F face){
		for(E edge : getEdgeIt(face)) {
			if(isBoundary(getTwin(edge))) {
				return Optional.of(edge);
			}
		}

		return Optional.empty();
	}

	boolean isBoundary(@NotNull E halfEdge);

	default boolean isBorder(@NotNull E halfEdge) {
		return isBorder(getFace(halfEdge));
	}

	boolean isDestroyed(@NotNull F face);
	boolean isDestroyed(@NotNull E edge);
	boolean isDestroyed(@NotNull V vertex);

	default boolean isAlive(@NotNull V vertex) {
		return !isDestroyed(vertex);
	}
	default boolean isAlive(@NotNull E edge) {
		return !isDestroyed(edge);
	}
	default boolean isAlive(@NotNull F face) {
		return !isDestroyed(face);
	}

	void setTwin(@NotNull E halfEdge, @NotNull E twin);
	void setNext(@NotNull E halfEdge, @NotNull E next);
	void setPrev(@NotNull E halfEdge, @NotNull E prev);
	void setFace(@NotNull E halfEdge, @NotNull F face);

	void setEdge(@NotNull F face, @NotNull E edge);
	void setEdge(@NotNull V vertex, @NotNull E edge);
	void setVertex(@NotNull E halfEdge, @NotNull V vertex);

	E createEdge(@NotNull V vertex);
	E createEdge(@NotNull V vertex, @NotNull F face);
	F createFace();
	F createFace(boolean hole);
	P createPoint(double x, double y);
	V createVertex(double x, double y);
	V createVertex(P point);
	F getBorder();

	void insert(V vertex);

	void insertVertex(V vertex);

	default V insertVertex(double x, double y) {
		V vertex = createVertex(x, y);
		insertVertex(vertex);
		return vertex;
	}

	// TODO: name?
	default F createFace(V... points) {
		F face = createFace();
		F borderFace = getBorder();

		LinkedList<E> edges = new LinkedList<>();
		LinkedList<E> borderEdges = new LinkedList<>();
		for(V p : points) {
			E edge = createEdge(p, face);
			setEdge(p, edge);
			E borderEdge = createEdge(p, borderFace);
			edges.add(edge);
			borderEdges.add(borderEdge);
		}

		E edge = null;
		for(E halfEdge : edges) {
			if(edge != null) {
				setNext(edge, halfEdge);
			}
			edge = halfEdge;
		}
		setNext(edges.peekLast(), edges.peekFirst());

		edge = null;
		for(E halfEdge : borderEdges) {
			if(edge != null) {
				setPrev(edge, halfEdge);
			}
			edge = halfEdge;
		}
		setPrev(borderEdges.peekLast(), borderEdges.peekFirst());

		for(int i = 0; i < edges.size(); i++) {
			E halfEdge = edges.get(i);
			E twin = borderEdges.get((i + edges.size() - 1) % edges.size());
			setTwin(halfEdge, twin);
		}

		setEdge(face, edges.peekFirst());
		setEdge(borderFace, borderEdges.peekFirst());

		return face;
	}

	void toHole(@NotNull F face);
	void destroyFace(@NotNull F face);
	void destroyEdge(@NotNull E edge);
	void destroyVertex(@NotNull V vertex);

	List<F> getFaces();

	List<F> getFacesWithHoles();

	List<E> getBoundaryEdges();

	List<V> getBoundaryVertices();

	Stream<F> streamFaces(@NotNull final Predicate<F> predicate);

	default Stream<F> streamFacesParallel() {
		return streamFaces(f -> true).parallel();
	}

	default Stream<F> streamFaces() {
		return streamFaces(f -> true);
	}

	/**
	 * Returns a Stream of holes.
	 *
	 * @return a Stream of holes.
	 */
	Stream<F> streamHoles();

	Stream<E> streamEdges();

	Stream<E> streamEdgesParallel();

	Stream<V> streamVertices();

	Stream<V> streamVerticesParallel();

	default Stream<P> streamPoints() {
		return streamVertices().map(v -> getPoint(v));
	}

	default Stream<P> streamPoints(F face) {
		return streamVertices(face).map(v -> getPoint(v));
	}

	default Stream<P> streamPointsParallel() {
		return streamEdgesParallel().map(e -> getPoint(e));
	}

	default VPolygon toPolygon(F face) {
		Path2D path2D = new Path2D.Double();
		E edge = getEdge(face);
		E prev = getPrev(edge);

		path2D.moveTo(getVertex(prev).getX(), getVertex(prev).getY());
		path2D.lineTo(getVertex(edge).getX(), getVertex(edge).getY());

		while (!edge.equals(prev)) {
			edge = getNext(edge);
			V p = getVertex(edge);
			path2D.lineTo(p.getX(), p.getY());
		}

		return new VPolygon(path2D);
	}

	default VTriangle toTriangle(F face) {
		List<V> vertices = getVertices(face);
		assert vertices.size() == 3;
		return new VTriangle(new VPoint(vertices.get(0)), new VPoint(vertices.get(1)), new VPoint(vertices.get(2)));
	}

	default Triple<P, P, P> toTriple(F face) {
		List<V> vertices = getVertices(face);
		assert vertices.size() == 3;
		return Triple.of(getPoint(vertices.get(0)), getPoint(vertices.get(1)), getPoint(vertices.get(2)));
	}

	default Optional<F> locate(final double x, final double y) {
		for(F face : getFaces()) {
			VPolygon polygon = toPolygon(face);
			if(polygon.contains(new VPoint(x, y))) {
				return Optional.of(face);
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns an iterator which iterates over all faces of this mesh.
	 *
	 * @return an iterator which iterates over all faces of this mesh
	 */
	@Override
	default Iterator<F> iterator() {
		return getFaces().iterator();
	}

	/**
	 * Returns a list of faces which are adjacent to the vertex of this edge.
	 *
	 * @param edge the edge holding the vertex
	 * @return a list of faces which are adjacent to the vertex of this edge
	 */
	default List<F> getFaces(@NotNull E edge) { return IteratorUtils.toList(new AdjacentFaceIterator(this, edge)); }

	/**
	 * Returns a list of faces which are adjacent to the vertex.
	 *
	 * @param vertex the vertex
	 * @return a list of faces which are adjacent to the vertex of this edge
	 */
	default List<F> getFaces(@NotNull V vertex) { return IteratorUtils.toList(new AdjacentFaceIterator(this, getEdge(vertex))); }

	/**
	 * Returns a Iterable which can be used to iterate over all edges which end point is the vertex that is adjacent to the vertex of this edge.
	 *
	 * @param edge the edge which holds the vertex
	 * @return a Iterable which can be used to iterate over all edges which are adjacent to the vertex of this edge.
	 */
	default Iterable<E> getIncidentEdgesIt(E edge) {
		return () -> new IncidentEdgeIterator(this, edge);
	}

	/**
	 * Returns a Iterable which can be used to iterate over adjacent vertices of this vertex.
	 *
	 * @param vertex the vertex
	 * @return a Iterable which can be used to iterate over all adjacent vertices.
	 */
	default Iterable<V> getAdjacentVertexIt(@NotNull final V vertex) {
		return () -> new AdjacentVertexIterator<>(this, vertex);
	}

	/**
	 * Returns an Iterable which can be used to iterate over all edges of a face.
	 *
	 * @param face the face the iterable iterates over
	 * @return an Iterable which can be used to iterate over all edges of a face.
	 */
	default Iterable<E> getEdgeIt(F face) {
		return () -> new EdgeIterator<>(this, face);
	}

	/**
	 * Returns an Iterable which can be used to iterate over all edges of a face which the edge is part of.
	 *
	 * @param edge the edge which is part of the face the iterable iterates over
	 * @return an Iterable which can be used to iterate over all edges of a face.
	 */
	default Iterable<E> getEdgeIt(E edge) {
		return () -> new EdgeIterator<>(this, edge);
	}

	/**
	 * Returns an Iterable which can be used to iterate over all vertices of a face.
	 *
	 * @param face the face the iterable iterates over
	 * @return an Iterable which can be used to iterate over all vertices of a face.
	 */
	default Iterable<V> getVertexIt(F face) {
		return () -> new VertexIterator<>(this, face);
	}

	/**
	 * Returns an Iterable which can be used to iterate over all vertices of a face.
	 *
	 * @param face the face the iterable iterates over
	 * @return an Iterable which can be used to iterate over all vertices of a face.
	 */
	default Iterable<P> getPointIt(F face) {
		return () -> new PointIterator<>(this, face);
	}

	/**
	 * Returns a Stream of edges of a face.
	 *
	 * @param face the faces of which edges the stream consist.
	 * @return a Stream of edges of a face.
	 */
	default Stream<E> streamEdges(F face) {
		Iterable<E> iterable = getEdgeIt(face);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a Stream of edges of a face.
	 *
	 * @param edge the edge of the face of which edges the stream consist.
	 * @return a Stream of edges of a face specified by the edge.
	 */
	default Stream<E> streamEdges(E edge) {
		Iterable<E> iterable = getEdgeIt(edge);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a Stream of vertices of a face.
	 *
	 * @param face the faces of which edges the stream consist.
	 * @return a Stream of edges of a face.
	 */
	default Stream<V> streamVertices(F face) {
		return streamEdges(face).map(edge -> getVertex(edge));
	}

	/**
	 * Returns an Iterable which can be used to iterate over surrounding faces of the face.
	 *
	 * @param face the face the iterable iterates over
	 * @return an Iterable which can be used to iterate over all surrounding faces.
	 */
	default Iterable<F> getFaceIt(F face) { return () -> new SurroundingFaceIterator<>(this, face);}

	default Iterable<F> getFaceIt(V vertex) { return () -> new AdjacentFaceIterator(this, getEdge(vertex));}

	default List<F> getFaces(F face) { return IteratorUtils.toList(new SurroundingFaceIterator<>(this, face)); }

	/**
	 * Returns a Stream consisting of all surrounding faces of the face.
	 *
	 * @param face the face of which surrounding faces the stream consist.
	 * @return a Stream consisting of all.
	 */
	default Stream<F> streamFaces(F face) {
		Iterable<F> iterable = getFaceIt(face);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	default Stream<F> streamFaces(V vertex) {
		Iterable<F> iterable = getFaceIt(vertex);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a Stream consisting of all edges which are incident to the edge
	 *
	 * @param edge the edge of which the edges are incident
	 * @return a Stream consisting of all edges which are incident to the edge.
	 */
	default Stream<E> streamIncidentEdges(E edge) {
		Iterable<E> iterable = getIncidentEdgesIt(edge);
		return StreamSupport.stream(iterable.spliterator(), false);
	}


	/**
	 * Returns an Iterable which can be used to iterate over all faces which are adjacent to the vertex of the edge
	 *
	 * @param edge the edge of which adjacent faces
	 * @return an Iterable which can be used to iterate over all faces which are adjacent to the vertex of the edge
	 */
	default Iterable<F> getAdjacentFacesIt(@NotNull E edge) { return () -> new AdjacentFaceIterator<>(this, edge); }

	/**
	 * Returns an Iterable which can be used to iterate over all faces which are adjacent to the vertex.
	 *
	 * @param vertex the vertex
	 * @return an Iterable which can be used to iterate over all faces which are adjacent to the vertex
	 */
	default Iterable<F> getAdjacentFacesIt(@NotNull V vertex) { return () -> new AdjacentFaceIterator<>(this, getEdge(vertex)); }

	/**
	 * Returns a List of all faces which are adjacent to the vertex of the edge
	 *
	 * @param edge the edge of which adjacent faces
	 * @return a List of all faces which are adjacent to the vertex of the edge
	 */
	default List<F> getAdjacentFaces(@NotNull E edge) {
		return IteratorUtils.toList(new AdjacentFaceIterator(this, edge));
	}

	/**
	 * Returns a list of edges which are incident to the vertex of this edge.
	 * They hold the vertices which are adjacent to vertex of the edge.
	 *
	 * @param edge the edge which holds the vertex
	 * @return a list of edges which are incident to the vertex of this edge.
	 */
	default List<E> getIncidentEdges(@NotNull E edge) { return IteratorUtils.toList(new IncidentEdgeIterator(this, edge)); }


	/**
	 * Returns an iterable that can be used to iterate over all edges which end-point is equal to the vertex, i.e. all edges connected to the vertex.
	 *
	 * @param vertex the end-point of all the edges
	 * @return an iterable that can be used to iterate over all edges which end-point is equal to the vertex
	 */
	default Iterable<E> getEdgeIt(@NotNull V vertex) {
		return () -> new EdgeOfVertexIterator(this, vertex);
	}

	/**
	 * Returns all edges which end-point is equal to the vertex, i.e. all edges connected to the vertex
	 *
	 * @param vertex the end-point of all the edges
	 * @return all edges which end-point is equal to the vertex
	 */
	default List<E> getEdges(@NotNull V vertex) {
		return IteratorUtils.toList(new EdgeOfVertexIterator(this, vertex));
	}

	/**
	 * Returns all edges which end-point is equal to the vertex, i.e. all edges connected to the vertex
	 *
	 * @param vertex the end-point of all the edges
	 * @return all edges which end-point is equal to the vertex
	 */
	default Stream<E> streamEdges(@NotNull V vertex) {
		Iterable<E> iterable = getEdgeIt(vertex);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a list of vertices which are adjacent to the vertex of this edge.
	 *
	 * @param edge the edge which holds the vertex
	 * @return a list of vertices which are adjacent to the vertex of this edge.
	 */
	default List<V> getAdjacentVertices(@NotNull E edge) {
		return streamIncidentEdges(edge).map(this::getVertex).collect(Collectors.toList());
	}

	default List<E> getEdges() {
		List<E> edges = new ArrayList<>();
		for (E edge : getEdgeIt()) {
			edges.add(edge);
		}

		return edges;
	}

	default Set<VLine> getLines() {
		return streamEdges().map(edge -> toLine(edge)).collect(Collectors.toSet());
	}

	default Set<VPoint> getUniquePoints() {
		return streamVertices().map(vertex -> toPoint(vertex)).collect(Collectors.toSet());
	}

	default Collection<P> getPoints() {
		return streamVertices().map(vertex -> getPoint(vertex)).collect(Collectors.toList());
	}

	Iterable<E> getEdgeIt();



	/**
	 * Returns a list of all edges of a face.
	 *
	 * @param face the face
	 * @return a list of all edges of a face.
	 */
	default List<E> getEdges(@NotNull F face) {
		return IteratorUtils.toList(new EdgeIterator(this, face));
	}

	/**
	 * Returns a list of all vertices of a face.
	 *
	 * @param face the face
	 * @return a list of all vertices of a face.
	 */
	default List<V> getVertices(@NotNull F face) {
		EdgeIterator<P, V, E, F> edgeIterator = new EdgeIterator<>(this, face);

		List<V> vertices = new ArrayList<>();
		while (edgeIterator.hasNext()) {
			vertices.add(getVertex(edgeIterator.next()));
		}

		return vertices;
	}

	/**
	 * Returns a list of all points of a face.
	 *
	 * @param face the face
	 * @return a list of all points of a face.
	 */
	default List<P> getPoints(@NotNull F face) {
		EdgeIterator<P, V, E, F> edgeIterator = new EdgeIterator<>(this, face);

		List<P> points = new ArrayList<>();
		while (edgeIterator.hasNext()) {
			points.add(getPoint(edgeIterator.next()));
		}

		return points;
	}

	/**
	 * Tests whether the point (x,y) is a vertex of the face.
	 *
	 * @param face
	 * @param x
	 * @param y
	 * @return
	 */
	default boolean isMember(F face, double x, double y) {
		return getMemberEdge(face, x, y).isPresent();
	}

	default boolean isMember(F face, double x, double y, double epsilon) {
		return getMemberEdge(face, x, y, epsilon).isPresent();
	}

	default Optional<E> getMemberEdge(F face, double x, double y) {
		return streamEdges(face).filter(e -> getVertex(e).getX() == x && getVertex(e).getY() == y).findAny();
	}

	default Optional<E> getMemberEdge(F face, double x, double y, double epsilon) {
		return streamEdges(face).filter(e -> getVertex(e).distance(x, y) <= epsilon).findAny();
	}

	// TODO: rename?
	default Optional<E> getEdgeCloseToVertex(F face, double x, double y, double epsilon) {
		for(E halfEdge : getEdgeIt(face)) {
			V p1 = getVertex(halfEdge);
			V p2 = getVertex(getPrev(halfEdge));

			if(Math.abs(GeometryUtils.ccw(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, y)) < epsilon) {
				return Optional.of(halfEdge);
			}
		}
		return Optional.empty();
	}

	Collection<V> getVertices();

	int getNumberOfVertices();

	int getNumberOfFaces();

	int getNumberOfEdges();

	static <P extends IPoint> IMesh<P, PVertex<P>, PHalfEdge<P>, PFace<P>> createPMesh(final IPointConstructor<P> pointConstructor) {
		return new PMesh<>(pointConstructor);
	}

	default V closestVertex(final F face, final double x, final double y) {
		V result = null;
		double distance = Double.MAX_VALUE;
		for (V vertex : getVertexIt(face)) {
			if(getPoint(vertex).distance(x, y) < distance) {
				result = vertex;
			}
		}

		return result;
	}

	boolean tryLock(@NotNull final V vertex);

	void unlock(@NotNull final V vertex);

	/**
	 * Returns the edge of a given face which is the closest edge of the face in respect to the point defined
	 * by (x,y). The point might be outside or inside the face or even on an specific edge.
	 *
	 * @param face  the face
	 * @param x     x-coordinate of the point
	 * @param y     y-coordinate of the point
	 * @return the edge of a given face which is closest to a point p = (x,y)
	 */
	default E closestEdge(final F face, final double x, final double y) {
		E result = null;
		double minDistance = Double.MAX_VALUE;
		for (E edge : getEdgeIt(face)) {
			double distance = GeometryUtils.distanceToLineSegment(getPoint(getPrev(edge)), getPoint(edge), x, y);
			if(distance < minDistance) {
				result = edge;
				minDistance = distance;
			}
		}

		return result;
	}

	IMesh<P, V, E, F> clone();

	ITriangulation<P, V, E, F> toTriangulation();

	/**
	 * Rearranges the memory location of faces, vertices and halfEdges of the mesh according to
	 * the {@link Iterable} faceOrder. I.e. edges, vertices and faces which are close the faceOrder
	 * will be close in the memory!
	 *
	 * Assumption: faceOrder contains all faces of this mesh.
	 * Invariant: the geometry i.e. the connectivity and the vertex positions will not change.
	 *
	 * @param faceOrder the new order
	 */
	void arrangeMemory(@NotNull Iterable<AFace<P>> faceOrder);


	// methods for log informations
	default String toPath(final F face) {
		return streamPoints(face).map(p -> p.toString()).reduce((s1, s2) -> s1 + " -> " + s2).orElse("");
	}
}
