package org.vadere.util.geometry.mesh.inter;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.impl.PMesh;
import org.vadere.util.geometry.mesh.iterators.EdgeIterator;
import org.vadere.util.geometry.mesh.iterators.AdjacentFaceIterator;
import org.vadere.util.geometry.mesh.iterators.IncidentEdgeIterator;
import org.vadere.util.geometry.mesh.iterators.SurroundingFaceIterator;
import org.vadere.util.geometry.shapes.IPoint;
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
public interface IMesh<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F> {
	E getNext(@NotNull E halfEdge);
	E getPrev(@NotNull E halfEdge);
	E getTwin(@NotNull E halfEdge);
	F getFace(@NotNull E halfEdge);

	E getEdge(@NotNull P vertex);
	E getEdge(@NotNull F face);

	P getVertex(@NotNull E halfEdge);

	default F getTwinFace(@NotNull E halfEdge) {
		return getFace(getTwin(halfEdge));
	}
	F getFace();

	/**
	 * Returns true if the face is the boundar
	 *
	 * @param face
	 * @return
	 */
	boolean isBoundary(@NotNull F face);
	boolean isBoundary(@NotNull E halfEdge);

	boolean isHole(@NotNull F face);
	boolean isHole(@NotNull E halfEdge);

	boolean isDestroyed(@NotNull F face);
	boolean isDestroyed(@NotNull E edge);

	void setTwin(@NotNull E halfEdge, @NotNull E twin);
	void setNext(@NotNull E halfEdge, @NotNull E next);
	void setPrev(@NotNull E halfEdge, @NotNull E prev);
	void setFace(@NotNull E halfEdge, @NotNull F face);

	void setEdge(@NotNull F face, @NotNull E edge);
	void setEdge(@NotNull P vertex, @NotNull E edge);
	void setVertex(@NotNull E halfEdge, @NotNull P vertex);

	E createEdge(@NotNull P vertex);
	E createEdge(@NotNull P vertex, @NotNull F face);
	F createFace();
	F createFace(boolean boundary);
	P createVertex(double x, double y);
	void insert(P vertex);

	void insertVertex(P vertex);

	default P insertVertex(double x, double y) {
		P vertex = createVertex(x, y);
		insertVertex(vertex);
		return vertex;
	}

	// TODO: name?
	default F createFace(P... points) {
		F superTriangle = createFace();
		F borderFace = createFace(true);

		LinkedList<E> edges = new LinkedList<>();
		LinkedList<E> borderEdges = new LinkedList<>();
		for(P p : points) {
			E edge = createEdge(p, superTriangle);
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

		setEdge(superTriangle, edges.peekFirst());
		setEdge(borderFace, borderEdges.peekFirst());

		return superTriangle;
	}

	void destroyFace(@NotNull F face);
	void destroyEdge(@NotNull E edge);
	void destroyVertex(@NotNull P vertex);

	List<F> getFaces();

	default VPolygon toPolygon(F face) {
		Path2D path2D = new Path2D.Double();
		E edge = getEdge(face);
		E prev = getPrev(edge);

		path2D.moveTo(getVertex(prev).getX(), getVertex(prev).getY());
		path2D.lineTo(getVertex(edge).getX(), getVertex(edge).getY());

		while (!edge.equals(prev)) {
			edge = getNext(edge);
			P p = getVertex(edge);
			path2D.lineTo(p.getX(), p.getY());
		}

		return new VPolygon(path2D);
	}

	default VTriangle toTriangle(F face) {
		List<P> vertices = getVertices(face);
		assert vertices.size() == 3;
		return new VTriangle(new VPoint(vertices.get(0)), new VPoint(vertices.get(1)), new VPoint(vertices.get(2)));
	}

	default Triple<P, P, P> toTriple(F face) {
		List<P> vertices = getVertices(face);
		assert vertices.size() == 3;
		return Triple.of(vertices.get(0), vertices.get(1), vertices.get(2));
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
	 * Returns a Iterable which can be used to iterate over all edges which are adjacent to the vertex of this edge.
	 *
	 * @param edge the edge which holds the vertex
	 * @return a Iterable which can be used to iterate over all edges which are adjacent to the vertex of this edge.
	 */
	default Iterable<E> getIncidentEdgesIt(E edge) {
		return () -> new IncidentEdgeIterator(this, edge);
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
	 * Returns an Iterable which can be used to iterate over surrounding faces of the face.
	 *
	 * @param face the face the iterable iterates over
	 * @return an Iterable which can be used to iterate over all surrounding faces.
	 */
	default Iterable<F> getFaceIt(F face) { return () -> new SurroundingFaceIterator<>(this, face);}

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
	 * Returns a list of vertices which are adjacent to the vertex of this edge.
	 *
	 * @param edge the edge which holds the vertex
	 * @return a list of vertices which are adjacent to the vertex of this edge.
	 */
	default List<P> getAdjacentVertices(@NotNull E edge) {
		return streamIncidentEdges(edge).map(this::getVertex).collect(Collectors.toList());
	}

	/**
	 * Returns all edges which end-point is equal to the vertex
	 *
	 * @param vertex the end-point of all the edges
	 * @return all edges which end-point is equal to the vertex
	 */
	List<E> getEdges(@NotNull P vertex);

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
	default List<P> getVertices(@NotNull F face) {
		EdgeIterator<P, E, F> edgeIterator = new EdgeIterator<>(this, face);

		List<P> vertices = new ArrayList<P>();
		while (edgeIterator.hasNext()) {
			vertices.add(getVertex(edgeIterator.next()));
		}

		return vertices;
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

	Collection<P> getVertices();

	int getNumberOfVertices();

	static <P extends IPoint> IMesh<P, PHalfEdge<P>, PFace<P>> createPMesh(final IPointConstructor<P> pointConstructor) {
		return new PMesh<>(pointConstructor);
	}
}
