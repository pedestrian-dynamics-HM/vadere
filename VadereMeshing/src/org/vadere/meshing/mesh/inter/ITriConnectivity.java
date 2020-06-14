package org.vadere.meshing.mesh.inter;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.DelaunayHierarchy;
import org.vadere.meshing.mesh.gen.GenEar;
import org.vadere.util.data.Node;
import org.vadere.util.data.NodeLinkedList;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>A tri-connectivity {@link ITriConnectivity} is the connectivity of a mesh of non-intersecting connected triangles including holes.
 * A hole can be an arbitrary simple polygon. So it is more concrete than a poly-connectivity {@link IPolyConnectivity}.
 * The mesh {@link IMesh} stores all the date of the base elements (vertices {@link V}, half-edges {@link E}
 * and faces {@link F}) and offers factory method to create new base elements.
 * The connectivities, i.e. {@link IPolyConnectivity} and {@link ITriConnectivity} offers all the operations manipulating
 * the connectivity of the mesh. The connectivity is the relation between vertices and edges which define faces which therefore define the mesh structure.</p>
 *
 * <p>We say a mesh represents a valid triangulation or a triangulation is valid if and only if all triangle-faces are counter-clockwise oriented.</p>
 *
 * <p>We say a mesh represents a feasible triangulation or a triangulation is feasible if and only if all triangle-faces are legal, i.e. all half-edges are legal.
 * The certificate for an edge to be legal relies on the concrete implementation of the triangulation. E. g. for a strict Delaunay-Triangulation the Delaunay-Criterion
 * has to be fulfilled.</p>
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 */
public interface ITriConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> extends IPolyConnectivity<V, E, F> {

	/**
	 * A logger for debug and information reasons.
	 */
	Logger log = Logger.getLogger(ITriConnectivity.class);

	/**
	 * A Random number generator to randomly walk through the trinagulation.
	 */
	Random random = new Random();

	/**
	 * A flag to activate and deactivate the debug mode.
	 */
	boolean debug = false;


	/**
	 * <p>This will be called whenever a triangle / face is split into three faces
	 * and inform all listeners about that event.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param original  the original face
	 * @param f1        one of the split results
	 * @param f2        one of the split results
	 * @param f3        one of the split results
	 * @param v         the vertex inserted
	 */
	default void splitTriangleEvent(@NotNull final F original, @NotNull final F f1, @NotNull final F f2, @NotNull final F f3, @NotNull final V v) {}

	/**
	 * <p>This will be called whenever a triangle / face is split at a specific edge which
	 * will split it into tow faces. The method informs all listeners about that event.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param originalEdge  the original edge which is split
	 * @param original      the original face
	 * @param f1            one of the split results
	 * @param f2            one of the split results
	 * @param v             the vertex inserted
	 */
	default void splitEdgeEvent(@NotNull E originalEdge, @NotNull final F original, @NotNull final F f1, @NotNull final F f2, @NotNull final V v) {}

	/**
	 * <p>This will replace the point of a vertex. If the point has other coordinates than
	 * the old point of the vertex this will reposition the vertex without any checks, i.e.
	 * the user has to know what he does and has to make sure that the mesh is valid and feasible
	 * afterwards and all listeners e.g. the point locators such as the Delaunay-Hierarchy
	 * {@link DelaunayHierarchy} can handle this
	 * repositioning!</p>
	 *
	 * <p>Does not change the connectivity but may change the position of a vertex and therefore requires
	 * connectivity changes which has to be made manually!</p>
	 *
	 * @param vertex    the vertex
	 * @param point     the new point of the vertex
	 */
	default void replacePoint(@NotNull final V vertex, @NotNull final IPoint point) {
		assert ringContainsPoint(vertex, point);
		getMesh().setPoint(vertex, point);
	}

	/**
	 * <p>Tests if the point is contained in the 1-Ring of the vertex, i.e. the polygon spanned by the
	 * neighbour points of the point including itself if the point is at the boundary.</p>
	 *
	 * @param vertex    the vertex
	 * @param point     the point
	 * @return true if the point is contained, false otherwise
	 */
	default boolean ringContainsPoint(@NotNull final V vertex, @NotNull final IPoint point)  {
		java.util.List<IPoint> points = getMesh().getPoints(vertex);

		if(getMesh().isAtBoundary(vertex)) {
			points.add(getMesh().toPoint(vertex));
		}

		double distance = GeometryUtils.toPolygon(points).distance(point);

		return points.contains(point)
				|| GeometryUtils.toPolygon(points).contains(point)
				|| GeometryUtils.toPolygon(points).distance(point) <= GeometryUtils.DOUBLE_EPS;
	}

	/**
	 * <p>Returns true if the full-edge of this half-edge is the longest edge of its faces.</p>
	 *
	 * @param edge the half-edge
	 * @return true if the full-edge of this half-edge is the longest edge of its faces
	 */
	default boolean isLongestEdge(@NotNull final E edge) {

		E e = edge;
		if(getMesh().isBoundary(e)) {
			e = getMesh().getTwin(e);
		}

		VLine line = getMesh().toLine(e);
		double lenSq = line.lengthSq();

		E next = getMesh().getNext(e);
		E prev = getMesh().getPrev(e);

		if(getMesh().toLine(next).lengthSq() > lenSq || getMesh().toLine(prev).lengthSq() > lenSq) {
			return false;
		}

		if(getMesh().isAtBoundary(e)) {
			return true;
		}
		else {
			e = getMesh().getTwin(e);
			next = getMesh().getNext(e);
			prev = getMesh().getPrev(e);
			return getMesh().toLine(next).lengthSq() < lenSq && getMesh().toLine(prev).lengthSq() < lenSq;
		}
	}

	default E getLongestHalfEdge(@NotNull final F face) {
		assert !getMesh().isBoundary(face);
		E edge = getMesh().getEdge(face);
		E next = getMesh().getNext(edge);
		E prev = getMesh().getPrev(edge);

		double len = getMesh().toLine(edge).lengthSq();
		double lenN = getMesh().toLine(next).lengthSq();
		double lenP = getMesh().toLine(prev).lengthSq();

		if(len >= lenN) {
			if(len >= lenP || lenP <= lenN) {
				return edge;
			} else {
				return prev;
			}
		} else {
			if(lenN > lenP) {
				return next;
			} else {
				return  prev;
			}
		}
	}

	default boolean isLongestHalfEdge(@NotNull final E edge) {
		E e = edge;
		if(getMesh().isBoundary(e)) {
			e = getMesh().getTwin(e);
		}

		E next = getMesh().getNext(e);
		E prev = getMesh().getPrev(e);
		VLine line = getMesh().toLine(e);
		double lenSq = line.lengthSq();

		return getMesh().toLine(next).lengthSq() <= lenSq && getMesh().toLine(prev).lengthSq() <= lenSq;
	}

	default boolean isShortestHalfEdge(@NotNull final E edge) {
		E e = edge;
		if(getMesh().isBoundary(e)) {
			e = getMesh().getTwin(e);
		}

		E next = getMesh().getNext(e);
		E prev = getMesh().getPrev(e);
		VLine line = getMesh().toLine(e);
		double lenSq = line.lengthSq();

		return getMesh().toLine(next).lengthSq() >= lenSq && getMesh().toLine(prev).lengthSq() >= lenSq;
	}

	default Set<V> getVertices(@NotNull final double x, final double y, final F startFace, @NotNull final Predicate<V> predicate) {
		assert !getMesh().isBoundary(startFace) && getMesh().toTriangle(startFace).contains(x, y);
		Set<V> set = new HashSet<>();
		LinkedList<V> heap = new LinkedList();
		for(V v : getMesh().getVertexIt(startFace)) {
			heap.addLast(v);
		}

		while (!heap.isEmpty()) {
			V candidate = heap.poll();
			if(predicate.test(candidate) && !set.contains(candidate)) {
				set.add(candidate);
				for(V neighbour : getMesh().getAdjacentVertexIt(candidate)) {
					heap.addLast(neighbour);
				}
			}
		}

		return set;
	}

	/**
	 * <p>Inserts a point into the mesh which is contained in a boundary by connecting the boundaryEdge
	 * to the point in O(1) time. This will create 4 new half-edges, one new vertex and one face.</p>
	 *
	 * <p>Assumption: The point is contained in the boundary i.e. the point is inside the border or inside a hole.</p>
	 *
	 * <p>Changes the connectivity.</p>
	 *
	 * @param point         the point to be inserted
	 * @param boundaryEdge  the boundary edge
	 * @param boundary      the boundary of the edge
	 * @return the created face
	 */
	default F insertOutsidePoint(@NotNull final IPoint point, @NotNull final E boundaryEdge, @NotNull final F boundary) {
		assert getMesh().isBoundary(boundaryEdge) &&
				getMesh().isBoundary(boundary) &&
				getMesh().getFace(boundaryEdge).equals(boundary) &&
				(!getMesh().locate(point.getX(), point.getY()).isPresent() || getMesh().locate(point.getX(), point.getY()).get().equals(boundary));

		V vertex = getMesh().createVertex(point);
		F face = getMesh().createFace();
		F borderFace = getMesh().getFace(boundaryEdge);

		E prev = getMesh().getPrev(boundaryEdge);
		E next = getMesh().getNext(boundaryEdge);

		E e1 = getMesh().createEdge(vertex);
		getMesh().setFace(e1, face);
		E e2 = getMesh().createEdge(getMesh().getVertex(prev));
		getMesh().setFace(e2, face);

		E b1 = getMesh().createEdge(vertex);
		getMesh().setFace(b1, borderFace);
		E b2 = getMesh().createEdge(getMesh().getVertex(boundaryEdge));
		getMesh().setFace(b2, borderFace);

		getMesh().setNext(prev, b1);
		getMesh().setNext(b1, b2);
		getMesh().setNext(b2, next);

		getMesh().setNext(boundaryEdge, e1);
		getMesh().setNext(e1, e2);
		getMesh().setNext(e2, boundaryEdge);

		getMesh().setTwin(b1, e2);
		getMesh().setTwin(b2, e1);

		getMesh().setEdge(vertex, e1);
		getMesh().setEdge(borderFace, b1);
		getMesh().setEdge(face, e1);

		getMesh().setFace(boundaryEdge, face);

		return face;
	}

	/**
	 * <p>This will be called whenever a triangle / face edge is flipped
	 * and inform all listeners about that event. For each flip two
	 * triangles are taking part.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param f1 the first triangle / face of the flip operation
	 * @param f2 the second triangle / face of the flip operation
	 */
	default void flipEdgeEvent(@NotNull final F f1, @NotNull final F f2) {}

	/**
	 * <p>This will be called whenever a new point is inserted into the mesh.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param vertex the vertex which was inserted
	 */
	default void insertEvent(@NotNull final E vertex) {};

	/**
	 * <p>Tests whether an edge is illegal and should be flipped.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param edge  the edge which will be tested
	 * @param p     the point, i.e. point(next(edge))
	 * @return true if the edge is illega, false otherwise
	 */
	boolean isIllegal(@NotNull final E edge, @NotNull final V p);

	boolean isIllegal(@NotNull final E edge, @NotNull final V p, final double eps);

	/**
	 * <p>Tests whether an edge is illegal and should be flipped.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param edge  the edge which will be tested
	 * @return true if the edge is illega, false otherwise
	 */
	default boolean isIllegal(@NotNull final E edge) {
		return isIllegal(edge, getMesh().getVertex(getMesh().getNext(edge)));
	}

	default boolean isIllegal(@NotNull final E edge, final double eps) {
		return isIllegal(edge, getMesh().getVertex(getMesh().getNext(edge)), eps);
	}

	default boolean isDelaunayIllegal(@NotNull final E edge) {
		return isDelaunayIllegal(edge, getMesh().getVertex(getMesh().getNext(edge)));
	}

	default boolean isDelaunayIllegal(@NotNull final E edge, @NotNull final V p, final double eps) {
		//assert mesh.getVertex(mesh.getNext(edge)).equals(p);
		//V p = mesh.getVertex(mesh.getNext(edge));
		E t0 = getMesh().getTwin(edge);
		E t1 = getMesh().getNext(t0);
		E t2 = getMesh().getNext(t1);

		V x = getMesh().getVertex(t0);
		V y = getMesh().getVertex(t1);
		V z = getMesh().getVertex(t2);

		//return Utils.angle3D(x, y, z) + Utils.angle3D(x, p, z) > Math.PI;

		//return Utils.isInCircumscribedCycle(x, y, z, p);
		//if(Utils.ccw(z,x,y) > 0) {
		return GeometryUtils.isInsideCircle(z, x, y, p, eps);
		//}
		//else {
		//	return Utils.isInsideCircle(x, z, y, p);
		//}
	}

	default boolean isDelaunayIllegal(@NotNull final E edge, @NotNull final V p) {
		return isDelaunayIllegal(edge, p, 0.0);
	}

	/**
	 * <p>Helper method which returns an arbitrary edge of a pair of edges.
	 * It returns the left if it is not null otherwise the right.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param pair a pair of half-edges
	 * @return an arbitrary edge of a pair of edges
	 */
    default E getAnyEdge(@NotNull final Pair<E, E> pair) {
        if(pair.getLeft() != null) {
            return pair.getLeft();
        }
        else {
            return pair.getRight();
        }
    }

	default Pair<E, E> splitEdge(@NotNull V v, @NotNull E halfEdge, boolean legalize) {
		IMesh<V, E, F> mesh = getMesh();
    	mesh.insertVertex(v);

		/*
		 * Situation: h0 = halfEdge
		 * h1 -> h2 -> h0
		 *       f0
		 * o2 <- o1 <- o0
		 *       f3
		 *
		 * After splitEdge:
		 * h0 -> h1 -> t0
		 *       f0
		 * t1 <- h2 <- e0
		 *       f1
		 *
		 * e1 -> o1 -> t2
		 *       f2
		 * o0 <- o2 <- e2
		 *       f3
		 */

		//h0,(t0),t1
		//e2,(o0,

		E h0 = halfEdge;
		E o0 = mesh.getTwin(h0);

		V v2 = mesh.getVertex(o0);
		F f0 = mesh.getFace(h0);
		F f3 = mesh.getFace(o0);

		// faces correct?
		//mesh.createEdge(v2, mesh.getFace(o0));
		E e1 = mesh.createEdge(v2, mesh.getFace(o0));
		E t2 = null;
		E t1 = mesh.createEdge(v, mesh.getFace(h0));

		mesh.setEdge(v, t1);
		mesh.setTwin(e1, t1);

		/*
		 * These two operations are strongly connected.
		 * Before these operations the vertex of o0 is v2.
		 * If the edge of v2 is equal to o0, the edge becomes
		 * invalid after calling mesh.setVertex(o0, v);
		 */
		mesh.setVertex(o0, v);
		if(mesh.getEdge(v2).equals(o0)) {
			mesh.setEdge(v2, e1);
		}

		F f1 = null;
		if(!mesh.isBoundary(h0)) {
			f1 = mesh.createFace();

			E h1 = mesh.getNext(h0);
			E h2 = mesh.getNext(h1);

			V v1 = mesh.getVertex(h1);
			E e0 = mesh.createEdge(v1, f1);
			E t0 = mesh.createEdge(v, f0);

			mesh.setTwin(e0, t0);

			mesh.setEdge(f0, h0);
			mesh.setEdge(f1, h2);

			mesh.setFace(h1, f0);
			mesh.setFace(t0, f0);
			mesh.setFace(h0, f0);

			mesh.setFace(h2, f1);
			mesh.setFace(t1, f1);
			mesh.setFace(e0, f1);

			mesh.setNext(h0, h1);
			mesh.setNext(h1, t0);
			mesh.setNext(t0, h0);

			mesh.setNext(e0, h2);
			mesh.setNext(h2, t1);
			mesh.setNext(t1, e0);
		}
		else {
			mesh.setNext(mesh.getPrev(h0), t1);
			mesh.setNext(t1, h0);
		}

		F f2 = null;
		if(!mesh.isBoundary(o0)) {
			E o1 = mesh.getNext(o0);
			E o2 = mesh.getNext(o1);

			V v3 = mesh.getVertex(o1);
			f2 = mesh.createFace();

			// face
			E e2 = mesh.createEdge(v3, mesh.getFace(o0));
			t2 = mesh.createEdge(v, f2);
			mesh.setTwin(e2, t2);

			mesh.setEdge(f2, o1);
			mesh.setEdge(f3, o0);

			mesh.setFace(o1, f2);
			mesh.setFace(t2, f2);
			mesh.setFace(e1, f2);

			mesh.setFace(o2, f3);
			mesh.setFace(o0, f3);
			mesh.setFace(e2, f3);

			mesh.setNext(e1, o1);
			mesh.setNext(o1, t2);
			mesh.setNext(t2, e1);

			mesh.setNext(o0, e2);
			mesh.setNext(e2, o2);
			mesh.setNext(o2, o0);
		}
		else {
			mesh.setNext(e1, mesh.getNext(o0));
			mesh.setNext(o0, e1);
		}

		// Event after the mesh connectivity is valid!
		insertEvent(t1);
		if(!mesh.isBoundary(h0)) {
			splitEdgeEvent(h0, f0, f0, f1, v);
		}

		if(!mesh.isBoundary(o0)) {
			splitEdgeEvent(o0, f3, f3, f2, v);
		}


		if(legalize) {
			if(!mesh.isBoundary(h0)) {
				E h1 = mesh.getNext(h0);
				E h2 = mesh.getPrev(t1);
				legalize(h1, v);
				legalize(h2, v);
			}

			if(!mesh.isBoundary(o0)) {
				E o1 = mesh.getNext(e1);
				E o2 = mesh.getPrev(o0);
				legalize(o1, v);
				legalize(o2, v);
			}
		}

		return Pair.of(t1, t2);
	}

	/**
	 * <p>Splits the half-edge at point p, which means two triangles will be split into four if
	 * the edge is not a boundary edge otherwise only one triangle will be split into two.</p>
	 *
	 * <p>Assumption: p is located on the edge!</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param p         the split point
	 * @param halfEdge  the half-edge which will be split
	 * @param legalize  if true the split will be legalized i.e. the mesh will be locally changed until it is legal
	 * @return one (the halfEdge is a boundary edge) or two halfEdges such that the set of faces of these
     *         edges and their twins are the faces which took part / where modified / added by the split.
     */
	default Pair<E, E> splitEdge(@NotNull IPoint p, @NotNull E halfEdge, boolean legalize) {
        IMesh<V, E, F> mesh = getMesh();
        V v = mesh.createVertex(p);
        return splitEdge(v, halfEdge, legalize);
	}

	default List<E> splitEdgeAndReturn(@NotNull final V v, @NotNull E halfEdge, boolean legalize) {
		IMesh<V, E, F> mesh = getMesh();
		mesh.insertVertex(v);

		/*
		 * Situation: h0 = halfEdge
		 * h1 -> h2 -> h0
		 *       f0
		 * o2 <- o1 <- o0
		 *       f3
		 *
		 * After splitEdge:
		 * h0 -> h1 -> t0
		 *       f0
		 * t1 <- h2 <- e0
		 *       f1
		 *
		 * e1 -> o1 -> t2
		 *       f2
		 * o0 <- o2 <- e2
		 *       f3
		 */

		//h0,(t0),t1
		//e2,(o0,

		E h0 = halfEdge;
		E o0 = mesh.getTwin(h0);

		V v2 = mesh.getVertex(o0);
		F f0 = mesh.getFace(h0);
		F f3 = mesh.getFace(o0);

		// faces correct?
		//mesh.createEdge(v2, mesh.getFace(o0));
		E e1 = mesh.createEdge(v2, mesh.getFace(o0));
		E t2 = null;
		E t1 = mesh.createEdge(v, mesh.getFace(h0));
		mesh.setEdge(v, t1);

		mesh.setTwin(e1, t1);

		/*
		 * These two operations are strongly connected.
		 * Before these operations the vertex of o0 is v2.
		 * If the edge of v2 is equal to o0, the edge becomes
		 * invalid after calling mesh.setVertex(o0, v);
		 */
		mesh.setVertex(o0, v);
		if(mesh.getEdge(v2).equals(o0)) {
			mesh.setEdge(v2, e1);
		}

		if(!mesh.isBoundary(h0)) {
			F f1 = mesh.createFace();

			E h1 = mesh.getNext(h0);
			E h2 = mesh.getNext(h1);

			V v1 = mesh.getVertex(h1);
			E e0 = mesh.createEdge(v1, f1);
			E t0 = mesh.createEdge(v, f0);

			mesh.setTwin(e0, t0);

			mesh.setEdge(f0, h0);
			mesh.setEdge(f1, h2);

			mesh.setFace(h1, f0);
			mesh.setFace(t0, f0);
			mesh.setFace(h0, f0);

			mesh.setFace(h2, f1);
			mesh.setFace(t1, f1);
			mesh.setFace(e0, f1);

			mesh.setNext(h0, h1);
			mesh.setNext(h1, t0);
			mesh.setNext(t0, h0);

			mesh.setNext(e0, h2);
			mesh.setNext(h2, t1);
			mesh.setNext(t1, e0);

			splitEdgeEvent(h0, f0, f0, f1, v);
		}
		else {
			mesh.setNext(mesh.getPrev(h0), t1);
			mesh.setNext(t1, h0);
		}

		if(!mesh.isBoundary(o0)) {
			E o1 = mesh.getNext(o0);
			E o2 = mesh.getNext(o1);

			V v3 = mesh.getVertex(o1);
			F f2 = mesh.createFace();

			// face
			E e2 = mesh.createEdge(v3, mesh.getFace(o0));
			t2 = mesh.createEdge(v, f2);
			mesh.setTwin(e2, t2);

			mesh.setEdge(f2, o1);
			mesh.setEdge(f3, o0);

			mesh.setFace(o1, f2);
			mesh.setFace(t2, f2);
			mesh.setFace(e1, f2);

			mesh.setFace(o2, f3);
			mesh.setFace(o0, f3);
			mesh.setFace(e2, f3);

			mesh.setNext(e1, o1);
			mesh.setNext(o1, t2);
			mesh.setNext(t2, e1);

			mesh.setNext(o0, e2);
			mesh.setNext(e2, o2);
			mesh.setNext(o2, o0);

			splitEdgeEvent(o0, f3, f3, f2, v);
		}
		else {
			mesh.setNext(e1, mesh.getNext(o0));
			mesh.setNext(o0, e1);
		}

		List<E> toLegalize = new ArrayList<>(4);

		if(!mesh.isBoundary(h0)) {
			E h1 = mesh.getNext(h0);
			E h2 = mesh.getPrev(t1);
			toLegalize.add(h1);
			toLegalize.add(h2);
		}

		if(!mesh.isBoundary(o0)) {
			E o1 = mesh.getNext(e1);
			E o2 = mesh.getPrev(o0);
			toLegalize.add(o1);
			toLegalize.add(o2);
		}

		if(legalize) {
			if(!mesh.isBoundary(h0)) {
				E h1 = mesh.getNext(h0);
				E h2 = mesh.getPrev(t1);
				legalize(h1, v);
				legalize(h2, v);
			}

			if(!mesh.isBoundary(o0)) {
				E o1 = mesh.getNext(e1);
				E o2 = mesh.getPrev(o0);
				legalize(o1, v);
				legalize(o2, v);
			}
		}

		return toLegalize;
	}

	/**
	 * <p>Splits the half-edge at point p, which means two triangles will be split into four if
	 * the edge is not a boundary edge otherwise only one triangle will be split into two.
	 * Afterwards the mesh is legalized locally, to preserve a feasible triangulation.</p>
	 *
	 * <p>Assumption: p is located on the edge!</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param p         the split point
	 * @param halfEdge  the half-edge which will be split
	 * @return one (the halfEdge is a boundary edge) or two halfEdges such that the set of faces of these
	 *         edges and their twins are the faces which took part / where modified / added by the split.
	 */
	default Pair<E, E> splitEdge(@NotNull final IPoint p, @NotNull final E halfEdge) {
		return splitEdge(p, halfEdge, true);
	}

	/**
	 * <p>Splits the half-edge at the mid point of its full-edge, which means two triangles will be split into four if
	 * the edge is not a boundary edge otherwise only one triangle will be split into two.</p>
	 *
	 * <p>Assumption: p is located on the edge!</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param halfEdge  the half-edge which will be split
	 * @param legalize  if true the split will be legalized i.e. the mesh will be locally changed until it is legal
	 * @return one (the halfEdge is a boundary edge) or two halfEdges such that the set of faces of these
	 *         edges and their twins are the faces which took part / where modified / added by the split.
	 */
	default Pair<E, E> splitEdge(@NotNull final E halfEdge, final boolean legalize) {
		return splitEdge(halfEdge, legalize, p -> {});
	}

	/**
	 * <p>Splits the half-edge at the mid point of its full-edge, which means two triangles will be split into four if
	 * the edge is not a boundary edge otherwise only one triangle will be split into two.</p>
	 *
	 * <p>Assumption: p is located on the edge!</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param halfEdge          the half-edge which will be split
	 * @param legalize          if true the split will be legalized i.e. the mesh will be locally changed until it is legal
	 * @return one (the halfEdge is a boundary edge) or two halfEdges such that the set of faces of these
	 *         edges and their twins are the faces which took part / where modified / added by the split.
	 *         Both edges ending in the inserted vertex i.e. getVertex(edge) returns the inserted vertex.
	 */
	default Pair<E, E> splitEdge(@NotNull final E halfEdge, final boolean legalize, @NotNull final Consumer<V> action) {
		VPoint midPoint = getMesh().toLine(halfEdge).midPoint();
		V v = getMesh().createVertex(midPoint.getX(), midPoint.getY());
		Pair<E, E> result = splitEdge(v, halfEdge, legalize);
		action.accept(v);
		return result;
	}


	/*default void flipLock(@NotNull final E edge) {

	}*/

	/**
	 * <p>A synchronized version of {@link ITriConnectivity#flip(IHalfEdge)}, i.e. the method acquires every
	 * involved vertex (four vertices) before it flips the edge.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param edge the edge which will be flipped.
	 */
	default void flipSync(@NotNull final E edge) {
		IMesh<V, E, F> mesh = getMesh();

		E a0 = edge;
		E a1 = mesh.getNext(a0);
		E a2 = mesh.getNext(a1);

		E b0 = mesh.getTwin(edge);
		E b1 = mesh.getNext(b0);

		V v1 = mesh.getVertex(a0);
		V v2 = mesh.getVertex(a1);
		V v3 = mesh.getVertex(a2);
		V v4 = mesh.getVertex(b1);

		// TODO: a very first and simple aquire all locks implementation => improve it
		while (true) {
			// lock all 4 involved vertices
			if (mesh.tryLock(v1)) {
				if (mesh.tryLock(v2)) {
					if (mesh.tryLock(v3)) {
						if (mesh.tryLock(v4)) {
							break;
						}
						else {
							mesh.unlock(v3);
							mesh.unlock(v2);
							mesh.unlock(v1);
						}
					}
					else {
						mesh.unlock(v2);
						mesh.unlock(v1);
					}
				} else {
					mesh.unlock(v1);
				}
			}

		}

		try {
			// if everything is locked flip
			flip(edge);
		}
		// unlock all locks
		finally {
			mesh.unlock(v4);
			mesh.unlock(v3);
			mesh.unlock(v2);
			mesh.unlock(v1);
		}
	}

	/**
	 * <p>Flips an edge in the triangulation assuming the egdge which will be created is not jet there.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param edge the edge which will be flipped.
	 */
	default void flip(@NotNull final E edge) {

		IMesh<V, E, F> mesh = getMesh();

		// 1. gather all the references required
		E a0 = edge;
		E a1 = mesh.getNext(a0);
		E a2 = mesh.getNext(a1);

		E b0 = mesh.getTwin(edge);
		E b1 = mesh.getNext(b0);
		E b2 = mesh.getNext(b1);

		F fa = mesh.getFace(a0);
		F fb = mesh.getFace(b0);

		V va1 = mesh.getVertex(a1);
		V vb1 = mesh.getVertex(b1);

		V va0 = mesh.getVertex(a0);
		V vb0 = mesh.getVertex(b0);

		if(mesh.getEdge(fb).equals(b1)) {
			mesh.setEdge(fb, a1);
		}

		if(mesh.getEdge(fa).equals(a1)) {
			mesh.setEdge(fa, b1);
		}

		// TODO: maybe without if, just do it? its faster?
		assert mesh.getVertex(b2) == va0;
		assert mesh.getVertex(a2) == vb0;

		if(mesh.getEdge(va0).equals(a0)) {
			mesh.setEdge(va0, b2);
		}

		if(mesh.getEdge(vb0).equals(b0)) {
			mesh.setEdge(vb0, a2);
		}

		mesh.setVertex(a0, va1);
		mesh.setVertex(b0, vb1);

		mesh.setNext(a0, a2);
		mesh.setNext(a2, b1);
		mesh.setNext(b1, a0);

		mesh.setNext(b0, b2);
		mesh.setNext(b2, a1);
		mesh.setNext(a1, b0);

		mesh.setFace(a1, fb);
		mesh.setFace(b1, fa);

		flipEdgeEvent(fa, fb);
	}

	/**
	 * <p>Tests if the face is counter-clockwise oriented in O(1) time. If a triangulation is valid
	 * all triangle-faces are counter-clockwise oriented.</p>
	 *
	 * <p>Assumption: The face is a triangle!</p>
	 *
	 * @param triangleFace the face representing a triangle
	 * @return true if the face (triangle) is counter-clockwise oriented, false otherwise
	 */
	default boolean isCCW(@NotNull final F triangleFace) {
		assert getMesh().getEdges(triangleFace).size() == 3;

		E edge = getMesh().getEdge(triangleFace);
		IPoint p1 = getMesh().getPoint(edge);
		IPoint p2 = getMesh().getPoint(getMesh().getNext(edge));
		IPoint p3 = getMesh().getPoint(getMesh().getPrev(edge));

		return GeometryUtils.isCCW(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
	}

	/**
	 * Inserts a new point into the mesh / triangulation by preserving a feasible triangulation. There are different possible
	 * outcomes:
	 * <ol>
	 *     <li>the face will be split by an edge-split {@link ITriConnectivity#splitEdge(IPoint, IHalfEdge)}</li>
	 *     <li>the face will be split by an face-split {@link ITriConnectivity#splitTriangle(IFace, IPoint)}</li>
	 *     <li>the point is very close to some point of the face {@link ITriConnectivity#isClose(double, double, IFace, double)}
	 *     and therefore it will not be inserted at all.</li>
	 * </ol>
	 * This requires amortized O(1) time.
	 *
	 * <p>Assumption:  the face contains the point or the point lines on an edge of the face
	 *              and the face is part of the mesh.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param p     the point which will be inserted
	 * @param face  the face which contains the point.
	 * @return one of the new created half-edges
	 */
	E insert(@NotNull final IPoint p, @NotNull final F face);

	default E splitTriangle(@NotNull final F face, final boolean legalize) {
		VPoint circumcenter = getMesh().toTriangle(face).getCircumcenter();
		return splitTriangle(face, getMesh().createPoint(circumcenter.getX(), circumcenter.getY()), legalize);
	}

	//TODO test it
	default void removeBoundaryVertex(@NotNull final V vertex) {
		assert getMesh().isAtBoundary(vertex);
		F boundary = getMesh().getFace(vertex);
		E boundaryEdge = getMesh().getBoundaryEdge(vertex).get();
		E next = getMesh().getNext(boundaryEdge);
		E nnext = getMesh().getNext(next);

		List<E> ringEdges = getMesh()
				.streamEdges(vertex)
				.map(edge -> getMesh().getPrev(edge))
				.collect(Collectors.toList());

		for(int i = 0; i < ringEdges.size()-1; i++) {
			E edge = ringEdges.get(i);
			V v = getMesh().getVertex(edge);
			getMesh().setNext(edge, ringEdges.get(i+1));
			getMesh().setFace(edge, boundary);
			// adjust since the edge is now a boundary edge!
			getMesh().setEdge(v, edge);
		}

		getMesh().setNext(ringEdges.get(ringEdges.size()-1), nnext);
	}

	/**
	 * Removes a non-boundary vertex from the triangulation by removing the point and re-triangulating the hole
	 * using the algorithm described in: On Deletion in Delaunay Triangulations.
	 *
	 * Assumption: the vertex is not at a boundary
	 *
	 * @param vertex the vertex which will be removed
	 */
	default void removeNonBoundaryVertex(@NotNull final V vertex) {
		assert !getMesh().isAtBoundary(vertex);

		// (1) remove the vertex
		// get ringEdges in ccw order!
		List<E> ringEdges = getMesh()
				.streamEdges(vertex)
				.map(edge -> getMesh().getPrev(edge)).collect(Collectors.toList());

		F face = getMesh().getFace(ringEdges.get(ringEdges.size()-1));

		for(int i = 0; i < ringEdges.size(); i++) {
			E edge = ringEdges.get(i);
			E next = getMesh().getNext(edge);
			E nextTwin = getMesh().getTwin(next);
			F f = getMesh().getFace(edge);

			getMesh().destroyEdge(next);
			getMesh().destroyEdge(nextTwin);
			if(i != ringEdges.size() -1) {
				getMesh().destroyFace(f);
			}
		}

		for(int i = 0; i < ringEdges.size(); i++) {
			E edge = ringEdges.get(i);
			getMesh().setNext(edge, ringEdges.get((i+1) % ringEdges.size()));
			getMesh().setFace(edge, face);

			E vEdge = getMesh().getEdge(getMesh().getVertex(edge));
			if(getMesh().isDestroyed(vEdge)/* || !getMesh().isAtBoundary(vEdge)*/) {
				getMesh().setEdge(getMesh().getVertex(edge), edge);
			}
		}

		getMesh().setEdge(face, ringEdges.get(ringEdges.size()-1));
		getMesh().setFace(ringEdges.get(ringEdges.size()-1), face);
		getMesh().destroyVertex(vertex);

		NodeLinkedList<GenEar<V, E, F>> list = new NodeLinkedList<>();
		GenEar.EarNodeComparator<V, E, F> comparator = new GenEar.EarNodeComparator<>();
		PriorityQueue<Node<GenEar<V, E, F>>> heap = new PriorityQueue<>(comparator);

		assert getMesh().isValid();
		// (2) re-triangulate
		for(int i = 0; i < ringEdges.size(); i++) {
			E e1 = ringEdges.get(i % ringEdges.size());
			E e2 = ringEdges.get((i+1) % ringEdges.size());
			E e3 = ringEdges.get((i+2) % ringEdges.size());

			GenEar<V, E, F> ear = new GenEar<>(e1, e2, e3, power(e1, e2, e3, vertex));
			Node<GenEar<V, E, F>> earNode = list.add(ear);
			heap.add(earNode);

		}

		while (heap.size() > 3) {
			Node<GenEar<V, E, F>> earNode = heap.poll();
			GenEar<V, E, F> ear = earNode.getElement();

			// create triangle ear and link it to its two or three existing neighbors
			E e1 = ear.getEdges().get(0);
			E e2 = ear.getEdges().get(1);
			E e3 = ear.getEdges().get(2);
			E next = getMesh().getNext(e3);

			E e = getMesh().createEdge(getMesh().getVertex(e1));
			E t = getMesh().createEdge(getMesh().getVertex(e3));
			F f = getMesh().createFace();
			F tf = getMesh().getFace(e1);

			getMesh().setEdge(f, e);
			getMesh().setTwin(e, t);
			getMesh().setNext(e, e2);
			getMesh().setNext(e3, e);

			getMesh().setFace(e, f);
			getMesh().setFace(e2, f);
			getMesh().setFace(e3, f);

			getMesh().setNext(t, next);
			getMesh().setNext(e1, t);
			getMesh().setFace(t, tf);
			getMesh().setEdge(tf, t);
			// end

			if(heap.size() > 3) {
				Node<GenEar<V, E, F>> prevEarNode = earNode.getPrev();
				Node<GenEar<V, E, F>> nextEarNode = earNode.getNext();

				if(prevEarNode == null) {
					prevEarNode = list.getTail();
				}

				if(nextEarNode == null) {
					nextEarNode = list.getHead();
				}

				Node<GenEar<V, E, F>> nnextEarNode = nextEarNode.getNext();
				if(nnextEarNode == null) {
					nnextEarNode = list.getHead();
				}


				heap.remove(earNode);
				heap.remove(prevEarNode);
				heap.remove(nextEarNode);
				heap.remove(nnextEarNode);

				prevEarNode.getElement().setLast(t);

				nextEarNode.getElement().setFirst(e1);
				nextEarNode.getElement().setMiddle(t);
				nnextEarNode.getElement().setFirst(t);
				earNode.remove();

				GenEar<V, E, F> prevEar = prevEarNode.getElement();
				GenEar<V, E, F> nextEar = nextEarNode.getElement();
				GenEar<V, E, F> nnextEar = nnextEarNode.getElement();
				prevEar.setPower(power(prevEar.getEdges().get(0), prevEar.getEdges().get(1), prevEar.getEdges().get(2), vertex));
				nextEar.setPower(power(nextEar.getEdges().get(0), nextEar.getEdges().get(1), nextEar.getEdges().get(2), vertex));
				nnextEar.setPower(power(nnextEar.getEdges().get(0), nnextEar.getEdges().get(1), nnextEar.getEdges().get(2), vertex));
				heap.add(prevEarNode);
				heap.add(nextEarNode);
				heap.add(nnextEarNode);
			}
		}
	}

	/**
	 * Removes a vertex from the triangulation by removing the point and re-triangulating the hole
	 * using the algorithm described in: On Deletion in Delaunay Triangulations.
	 *
	 * @param vertex the vertex which will be removed
	 */
	default void remove(@NotNull final V vertex) {
		if(getMesh().isAtBoundary(vertex)) {
			removeBoundaryVertex(vertex);
		} else {
			removeNonBoundaryVertex(vertex);
		}
	}

	private double power(@NotNull final E e1, @NotNull final E e2, @NotNull final E e3, @NotNull final IPoint p) {
		IPoint point = getMesh().getPoint(e1);
		if(!isLeftOf(point.getX(), point.getY(), e3)) {
			return Double.MAX_VALUE;
		}

		VPoint p1 = getMesh().toPoint(getMesh().getPoint(e1));
		VPoint p2 = getMesh().toPoint(getMesh().getPoint(e2));
		VPoint p3 = getMesh().toPoint(getMesh().getPoint(e3));
		VTriangle triangle = new VTriangle(p1, p2, p3);
		VPoint x = triangle.getCircumcenter();
		double r = triangle.getCircumscribedRadius();
		double xpSq = x.distanceSq(p);
		double power = (xpSq - r*r);
		return -power;
	}

	/**
	 * <p>Splits the triangle xyz into three new triangles xyp, yzp and zxp. This requires amortized O(1) time.</p>
	 *
	 * <p>Assumption: p is inside the face.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param face      the triangle face we split
	 * @param point     the point which splits the triangle
	 * @param legalize  if true the triangulation will be legalized locally at the split to preserve a feasible triangulation
	 *
	 * @return an half-edge which has point as its end-point
	 */
	default E splitTriangle(@NotNull F face, @NotNull final IPoint point, boolean legalize) {
		V p = getMesh().createVertex(point);
		return splitTriangle(face, p, legalize);
	}

	default E splitTriangle(@NotNull F face, @NotNull final V p, boolean legalize) {
		//assert isTriangle(face) && locate(point).get().equals(face);

		getMesh().insertVertex(p);
		IMesh<V, E, F> mesh = getMesh();

		F xyp = mesh.createFace();
		F yzp = mesh.createFace();

		//F zxp = mesh.createFace();
		F zxp = face;

		E zx = mesh.getEdge(face);
		E xy = mesh.getNext(zx);
		E yz = mesh.getNext(xy);

		V x = mesh.getVertex(zx);
		V y = mesh.getVertex(xy);
		V z = mesh.getVertex(yz);

		E yp = mesh.createEdge(p, xyp);
		mesh.setEdge(p, yp);

		E py = mesh.createEdge(y, yzp);
		mesh.setTwin(yp, py);

		E xp =  mesh.createEdge(p, zxp);
		E px =  mesh.createEdge(x, xyp);
		mesh.setTwin(xp, px);

		E zp = mesh.createEdge(p, yzp);
		E pz = mesh.createEdge(z, zxp);
		mesh.setTwin(zp, pz);

		mesh.setNext(zx, xp);
		mesh.setNext(xp, pz);
		mesh.setNext(pz, zx);

		mesh.setNext(xy, yp);
		mesh.setNext(yp, px);
		mesh.setNext(px, xy);

		mesh.setNext(yz, zp);
		mesh.setNext(zp, py);
		mesh.setNext(py, yz);

		mesh.setEdge(xyp, yp);
		mesh.setEdge(yzp, py);
		mesh.setEdge(zxp, xp);

		mesh.setFace(xy, xyp);
		mesh.setFace(zx, zxp);
		mesh.setFace(yz, yzp);


		// we reuse the face for efficiency
		//mesh.destroyFace(face);

		splitTriangleEvent(face, xyp, yzp, zxp, p);

		if(legalize) {
			legalize(zx, p);
			legalize(xy, p);
			legalize(yz, p);
		}

		return xp;
	}

	/**
	 * <p>Splits the triangle xyz into three new triangles xyp, yzp and zxp and legalizes all possibly illegal edges locally,
	 * which preserves a legal triangulation. This requires amortized O(1) time.</p>
	 *
	 * <p>Assumption: p is contained in the face.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param face      the triangle face we split
	 *
	 * @param p         the point which splits the triangle
	 * @return a list of all newly created face.
	 */
	default E splitTriangle(@NotNull final F face, @NotNull final IPoint p) {
		return splitTriangle(face, p, true);
	}

	default V collapseEdge(@NotNull final E edge, final boolean deleteIsolatededVertex) {
		IMesh<V, E, F> mesh = getMesh();
		E twin = mesh.getTwin(edge);

		// before changing connectivity change vertices.
		V replacedVertex = getMesh().getVertex(twin);
		V survivedVertex = getMesh().getVertex(edge);

		for(E e : getMesh().getEdgeIt(replacedVertex)) {
			getMesh().setVertex(e, survivedVertex);
		}

		if(getMesh().getEdge(survivedVertex).equals(edge)) {
			getMesh().setEdge(survivedVertex, getMesh().getTwin(getMesh().getNext(edge)));
		}

		F fa = mesh.getFace(edge);
		F fb = mesh.getFace(twin);

		F f4 = getMesh().getTwinFace(getMesh().getPrev(edge));
		F f5 = getMesh().getTwinFace(getMesh().getNext(twin));

		boolean isF4Boundary = getMesh().isBoundary(f4);
		boolean isF5Boundary = getMesh().isBoundary(f5);

		// survives
		E aNext = mesh.getNext(edge);
		E bNext = mesh.getNext(twin);
		E aPrev = mesh.getPrev(edge);
		// survives
		E bPrev = mesh.getPrev(twin);

		if(!getMesh().isBoundary(edge)) {
			E aPrevTwin = mesh.getTwin(aPrev);
			E aPrevTwinPrev = mesh.getPrev(aPrevTwin);
			E aPrevTwinNext = mesh.getNext(aPrevTwin);
			E next = getMesh().getNext(edge);
			V nextVertex = getMesh().getVertex(next);

			if(getMesh().getEdge(nextVertex).equals(aPrevTwin)) {
				getMesh().setEdge(nextVertex, next);
			}

			// adjust pointers
			getMesh().setNext(aNext, aPrevTwinNext);
			getMesh().setPrev(aNext, aPrevTwinPrev);
			getMesh().setFace(aNext, f4);
			getMesh().setEdge(f4, aNext);

			// destroy the rest
			getMesh().destroyFace(fa);
			getMesh().destroyEdge(aPrev);
			getMesh().destroyEdge(aPrevTwin);
		} else {
			getMesh().setNext(getMesh().getPrev(edge), getMesh().getNext(edge));
		}

		if(!getMesh().isBoundary(twin)) {
			E bNextTwin = mesh.getTwin(bNext);
			E bNextTwinNext = mesh.getNext(bNextTwin);
			E bNextTwinPrev = mesh.getPrev(bNextTwin);
			E prevTwin = getMesh().getTwin(mesh.getPrev(twin));
			V nextVertex = getMesh().getVertex(prevTwin);

			if(getMesh().getEdge(nextVertex).equals(bNext)) {
				getMesh().setEdge(nextVertex, prevTwin);
			}

			// adjust pointers
			getMesh().setNext(bPrev, bNextTwinNext);
			getMesh().setPrev(bPrev, bNextTwinPrev);
			getMesh().setFace(bPrev, f5);
			getMesh().setEdge(f5, bPrev);

			// destroy the rest
			getMesh().destroyFace(fb);
			getMesh().destroyEdge(bNext);
			getMesh().destroyEdge(bNextTwin);
		} else {
			getMesh().setNext(getMesh().getPrev(twin), getMesh().getNext(twin));
		}

		// destroy the rest
		getMesh().destroyEdge(edge);
		getMesh().destroyEdge(twin);

		if(deleteIsolatededVertex) {
			getMesh().destroyVertex(replacedVertex);
		} else {
			getMesh().setEdge(replacedVertex, null);
		}

		adjustVertex(survivedVertex);
		assert getMesh().isValid();

		return survivedVertex;
	}

	/**
	 * <p>This method collapses a three degree vertex which is at the boundary by removing the
	 * one edge (a simple link) which is not a boundary edge and by merging the two other boundary edges,
	 * i.e. two triangles will become one and the vertex will be deleted. This requires O(1) time
	 * since we assume a triangulation.</p>
	 *
	 * <p>Assumption: vertex is a three degree vertex, the edge ends in vertex, the edge is a non-boundary edge,
	 * the two other half-edges ending in the vertex are boundary edges</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param vertex                    the vertex where the edge ends
	 * @param deleteIsolatededVertex    if true the vertex will be removed from the mesh data structure.
	 */
	default void collapse3DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex) {
		assert getMesh().degree(vertex) == 3;
		Optional<E> toDeleteEdge = getMesh().streamEdges(vertex).filter(e -> !getMesh().isAtBoundary(e)).findAny();

		assert toDeleteEdge.isPresent();

		if(toDeleteEdge.isPresent()) {
			E edge = toDeleteEdge.get();
			assert getMesh().streamEdges(vertex).filter(e -> getMesh().isAtBoundary(e)).count() == 2;

			E halfEdge = edge;
			if(!getMesh().getVertex(halfEdge).equals(vertex)) {
				halfEdge = getMesh().getTwin(halfEdge);
			}

			if(!getMesh().getVertex(halfEdge).equals(vertex)) {
				throw new IllegalArgumentException(halfEdge + " does not end in " + vertex + ".");
			}

			removeSimpleLink(edge);
			remove2DVertex(vertex, deleteIsolatededVertex);
		}
		else {
			log.warn("Did not found any non-boundary half-edge. Something went wrong!");
		}
	}

	/**
	 * <p>This method collapses a four degree vertex which is not at the boundary</p>
	 *
	 * @param vertex
	 * @param deleteIsolatededVertex
	 */
	default void collapse4DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex) {
		assert getMesh().degree(vertex) == 4;

		E edge = getMesh().getEdge(vertex);
		E opp = getMesh().getNext(getMesh().getTwin(getMesh().getNext(edge)));

		F f1 = removeSimpleLink(edge);
		F f2 = removeSimpleLink(opp);
		remove2DVertex(vertex, deleteIsolatededVertex);
	}

	/**
	 * <p>Removes a two degree vertex by removing its two collapsing its two neighbouring edges which
	 * will remove two half-edges which is one full-edge in O(1)</p>
	 *
	 * <p>Assumption: the veterx is of degree equals two.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param vertex                    the 2-degree vertex which will be removed
	 * @param deleteIsolatededVertex    if true the vertex will be removed from the mesh data structure
	 */
	default E remove2DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex) {
		assert getMesh().degree(vertex) == 2;

		E survivor = getMesh().getEdge(vertex);
		E next = getMesh().getNext(survivor);

		E twin = getMesh().getTwin(survivor);
		E twinPrev = getMesh().getPrev(twin);
		getMesh().setNext(survivor, getMesh().getNext(next));
		getMesh().setPrev(twin, getMesh().getPrev(twinPrev));

		if(getMesh().getEdge(getMesh().getFace(survivor)).equals(next)) {
			getMesh().setEdge(getMesh().getFace(survivor), survivor);
		}

		if(getMesh().getEdge(getMesh().getFace(twin)).equals(twinPrev)) {
			getMesh().setEdge(getMesh().getFace(twin), twin);
		}

		getMesh().setVertex(survivor, getMesh().getVertex(next));

		if(getMesh().getEdge(getMesh().getVertex(next)).equals(next)) {
			getMesh().setEdge(getMesh().getVertex(next), survivor);
		}

		getMesh().destroyEdge(twinPrev);
		getMesh().destroyEdge(next);

		if(deleteIsolatededVertex) {
			getMesh().destroyVertex(vertex);
		}

		return survivor;
	}

	/**
	 * <p>Creates a new face by connecting two boundary vertices v1, v3 of a boundary path v1 to v2 to v3 such that
	 * v1 to v2 to v3 becomes a new face. This requires O(1) time.</p>
	 *
	 * Assumption:
	 * <ul>
	 *     <li>there is an counter clockwise angle3D smaller than 180 (PI) at v2 of the triangle (v1,v2,v3)</li>
	 *     <li>the boundaryEdge is a boundary edge</li>
	 * </ul>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param boundaryEdge an edge of the boundary (i.e. part of the border or a hole).
	 */
	default F createFaceAtBoundary(@NotNull final E boundaryEdge) {
		assert getMesh().isBoundary(boundaryEdge);

		F boundary = getMesh().getFace(boundaryEdge);
		E next = getMesh().getNext(boundaryEdge);
		E prev = getMesh().getPrev(boundaryEdge);

		// can we form a triangle
		assert GeometryUtils.isCCW(getMesh().toPoint(prev), getMesh().toPoint(boundaryEdge), getMesh().toPoint(next));

		E nnext = getMesh().getNext(next);

		E newEdge = getMesh().createEdge(getMesh().getVertex(next));
		E newTwin = getMesh().createEdge(getMesh().getVertex(prev));
		F newFace = getMesh().createFace();

		getMesh().setFace(newEdge, boundary);
		getMesh().setNext(newEdge, nnext);
		getMesh().setPrev(newEdge, prev);
		getMesh().setTwin(newEdge, newTwin);

		getMesh().setFace(newTwin, newFace);
		getMesh().setNext(newTwin, boundaryEdge);
		getMesh().setPrev(newTwin, next);
		getMesh().setTwin(newTwin, newEdge);

		getMesh().setEdge(newFace, newTwin);

		//getMesh().setPrev(nnext, newEdge);

		//getMesh().setNext(next, newTwin);
		//getMesh().setFace(newEdge, newFace);

		//getMesh().setPrev(boundaryEdge, newTwin);
		getMesh().setFace(boundaryEdge, newFace);
		getMesh().setFace(next, newFace);

		//getMesh().setNext(prev, newEdge);

		if(getMesh().getEdge(boundary).equals(boundaryEdge) || getMesh().getEdge(boundary).equals(next)) {
			getMesh().setEdge(boundary, newEdge);
		}

		// to find the boundary edge as quick as possible
		getMesh().setEdge(getMesh().getVertex(next), newEdge);

		assert getMesh().getVertices(newFace).size() == 3;
		return newFace;
	}

	/*default void collapse3DAtBoundary(@NotNull final V vertex, final boolean removeIsolatedVertex) {
		assert getMesh().degree(vertex) == 3;
		List<E> edges = getMesh().getEdges(vertex);

		if(edges.stream().filter(e -> getMesh().isAtBoundary(e)).count() != 2) {
			System.out.println(edges.stream().filter(e -> getMesh().isAtBoundary(e)).count());
		}
		assert edges.stream().filter(e -> getMesh().isAtBoundary(e)).count() == 2;

		F boundary = getMesh().streamFaces(vertex).filter(f -> getMesh().isBoundary(f)).findAny().get();

		E e1;
		E e2;
		E e3;
		if(!getMesh().isAtBoundary(edges.get(0))) {
			e1 = getMesh().isBoundary(edges.get(1)) ? edges.get(1) : edges.get(2);
			e2 = getMesh().isBoundary(edges.get(1)) ? edges.get(2) : edges.get(1);
			e3 = edges.get(0);
		}
		else if(!getMesh().isAtBoundary(edges.get(1))) {
			e1 = getMesh().isBoundary(edges.get(0)) ? edges.get(0) : edges.get(2);
			e2 = getMesh().isBoundary(edges.get(0)) ? edges.get(2) : edges.get(0);
			e3 = edges.get(1);
		}
		else if(!getMesh().isAtBoundary(edges.get(2))) {
			e1 = getMesh().isBoundary(edges.get(0)) ? edges.get(0) : edges.get(1);
			e2 = getMesh().isBoundary(edges.get(0)) ? edges.get(1) : edges.get(0);
			e3 = edges.get(1);
		}
		else {
			throw new IllegalArgumentException("the degree of " + vertex + " is > 3 and/or is is not at the boundary.");
		}
		E twinE3 = getMesh().getTwin(e3);
		E twinE2 = getMesh().getTwin(e2);

		// 1. adjust faces
		getMesh().setFace(getMesh().getPrev(e2), getMesh().getFace(e3));

		// if
		getMesh().setEdge(getMesh().getFace(e3), getMesh().getPrev(e3));

		// if
		getMesh().setEdge(boundary, e1);

		getMesh().destroyFace(getMesh().getFace(getMesh().getTwin(e3)));

		// 2. adjust vertex relation
		getMesh().setVertex(e1, getMesh().getVertex(getMesh().getNext(e1)));


		// 3. adjust connectivity
		getMesh().setNext(e1, getMesh().getNext(getMesh().getTwin(e2)));
		getMesh().setPrev(getMesh().getNext(getMesh().getTwin(e2)), e1);

		getMesh().setPrev(getMesh().getTwin(e1), getMesh().getPrev(e2));
		getMesh().setNext(getMesh().getPrev(e2), getMesh().getTwin(e1));

		getMesh().setNext(getMesh().getPrev(e3), getMesh().getNext(getMesh().getTwin(e3)));
		getMesh().setPrev(getMesh().getNext(getMesh().getTwin(e3)), getMesh().getPrev(e3));

		// if
		getMesh().setVertex(e1, getMesh().getVertex(twinE2));
		getMesh().setEdge(getMesh().getVertex(twinE2), e1);

		getMesh().destroyFace(getMesh().getFace(e2));
		getMesh().destroyEdge(e3);
		getMesh().destroyEdge(twinE3);
		getMesh().destroyEdge(e2);
		getMesh().destroyEdge(twinE2);

		if(removeIsolatedVertex) {
			getMesh().destroyVertex(vertex);
		}
	}*/

	/**
	 * <p>Legalizes recursively an edge xy of a triangle xyz if it is illegal / not feasible by flipping it.
	 * This requires amortized O(1) time.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param edge  an edge zx of a triangle xyz
	 * @param p     point(next(edge))
	 * @param depth the depth of the recursion
	 */
	default void legalizeRecursively(@NotNull final E edge, @NotNull final V p, int depth) {
		if(isIllegal(edge, p)) {
			assert isFlipOkAssertion(edge);
			assert getMesh().getVertex(getMesh().getNext(edge)).equals(p);

			F f1 = getMesh().getFace(edge);
			F f2 = getMesh().getTwinFace(edge);

			flip(edge);

			V vertex = getMesh().getVertex(edge);

			if(vertex.equals(p)) {
				E e1 = getMesh().getPrev(edge);
				E e2 = getMesh().getNext(getMesh().getTwin(edge));


				legalizeRecursively(e1, p, depth+1);
				legalizeRecursively(e2, p, depth+1);
			}
			else {
				E e1 = getMesh().getNext(edge);
				E e2 = getMesh().getPrev(getMesh().getTwin(edge));
				legalizeRecursively(e1, p, depth+1);
				legalizeRecursively(e2, p, depth+1);
			}


		}
	}

	/**
	 * <p>Legalizes an edge xy of a triangle xyz if it is illegal / not feasible by flipping it.
	 * This requires amortized O(1) time.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param edge  an edge zx of a triangle xyz
	 * @param p     point(next(edge))
	 */
	default void legalizeNonRecursive(@NotNull final E edge, @NotNull final V p) {
		int flips = 0;
		int its = 0;
		//if(isIllegal(edge, p)) {

		// this should be the same afterwards
		//E halfEdge = getMesh().getNext(edge);

		IMesh<V, E, F> mesh = getMesh();
		E startEdge = mesh.getPrev(edge);
		E endEdge = mesh.getTwin(getMesh().getPrev(startEdge));
		E currentEdge = mesh.getPrev(edge);

		// flipp
		//c.prev.twin

		while(currentEdge != endEdge) {
			while (isIllegal(mesh.getNext(currentEdge), p)) {
				flip(mesh.getNext(currentEdge));
				flips++;
				its++;
			}
			its++;

			currentEdge = mesh.getTwin(mesh.getPrev(currentEdge));
		}

		//log.info("#flips = " + flips);
		//log.info("#its = " + its);
		//}
	}

	/**
	 * <p>Legalizes an edge xy of a triangle xyz if it is illegal / not feasible by flipping it.
	 * This requires amortized O(1) time.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param edge  an edge zx of a triangle xyz
	 * @param p     point(next(edge))
	 */
	default void legalize(@NotNull  final E edge, @NotNull final V p) {
		legalizeNonRecursive(edge, p);
	}

	default void legalize(@NotNull  final E edge) {
		legalizeNonRecursive(edge, getMesh().getVertex(getMesh().getNext(edge)));
	}

	/**
	 * This method is a plausibility assertion-test. It tests if
	 * <ol>
	 *  <li>the edge is not a boundary edge</li>
	 *  <li>the vertex of the next of its twin is not equal to any vertex of the neighbouring edges of its next.</li>
	 * </ol>
	 * This method requires O(d) time where d is the degree of the involved vertices and should only be used for assertions.
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param halfEdge the half-edge that might be flipped
	 * @return true if the plausibility assertion-test is true.
	 */
	default boolean isFlipOkAssertion(@NotNull final E halfEdge) {
		if(getMesh().isBoundary(halfEdge)) {
			return false;
		}
		else {
			E xy = halfEdge;
			E yx = getMesh().getTwin(halfEdge);

			if(getMesh().getVertex(getMesh().getNext(xy)).equals(getMesh().getVertex(getMesh().getNext(yx)))) {
				return false;
			}

			V vertex = getMesh().getVertex(getMesh().getNext(yx));
			for(E neigbhour : getMesh().getIncidentEdgesIt(getMesh().getNext(xy))) {

				if(getMesh().getVertex(neigbhour).equals(vertex)) {
					return false;
				}
			}
		}
		return true;
	}

	default Optional<F> locate(final double x, final double y) {
		Optional<F> optFace;
		if(getMesh().getNumberOfFaces() > 1) {
			optFace = locateMarch(x, y, getMesh().getFace());
		}
		else if(getMesh().getNumberOfFaces() == 1) {
			optFace = Optional.of(getMesh().getFace());
		}
		else {
			optFace = Optional.empty();
		}

		return optFace;
	}

	/**
	 * <p>Searches and returns the face containing the point (x,y) in O(n),
	 * where n is the number of faces of the mesh by starting at a specific face.
	 * The search uses a robust straight walk such that it can walk through holes, i.e.
	 * polygons. If this face is close to (x, y) the search will be fast.</p>
	 *
	 * Assumption: the start-face is contained in the mesh structure.
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x         x-coordinate of the location point
	 * @param y         y-coordinate of the location point
	 * @param startFace the face at which the search starts
	 * @return the face containing the point or empty() if there is none
	 */
	default Optional<F> locateMarch(final double x, final double y, @NotNull final F startFace) {
		// there is no face.
		if(getDimension() <= 0 ){
			return Optional.empty();
		}

		if(getDimension() == 1) {
			return marchLocate1D(x, y, startFace);
		}
		else {
			return Optional.of(straightWalk2D(x, y, startFace));
		}
	}

	/**
	 * <p>Searches and returns (optional) the face containing the point (x,y) in O(n),
	 * where n is the number of faces of the mesh by starting at a specific face.
	 * The search uses a robust straight walk such that it can walk through holes, i.e.
	 * polygons. If this face is close to (x, y) the search will be fast.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param point     the point (x, y)
	 * @param startFace the face at which the search starts
	 * @return the face containing the point or empty() if there is none
	 */
	default Optional<F> locateMarch(@NotNull final IPoint point, F startFace) {
		return locateMarch(point.getX(), point.getY(), startFace);
	}

	/**
	 * <p>Straight walk in the 1D-case, i.e. the mesh consists only one interior-face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x         the x-coordinate of the point
	 * @param y         the y-coordinate of the point
	 * @param startFace the startFace face of the search
	 * @return (optional) the face containing the point or empty() if there is none
	 */
	default Optional<F> marchLocate1D(final double x, final double y, @NotNull final F startFace) {
		if(contains(x, y, startFace)) {
			return Optional.of(startFace);
		}
		else {
			return Optional.empty();
		}
	}

	// TODO: still required?
	default E walkThroughHole(@NotNull final VPoint q, @NotNull final VPoint p, @NotNull final E enteringEdge) {
		assert GeometryUtils.intersectLine(q, p, getMesh().getPoint(enteringEdge), getMesh().getPoint(getMesh().getPrev(enteringEdge)));
		E next = getMesh().getNext(enteringEdge);

		while (enteringEdge != next) {
			VPoint p1 = getMesh().toPoint(getMesh().getVertex(enteringEdge));
			VPoint p2 = getMesh().toPoint(getMesh().getVertex(getMesh().getPrev(enteringEdge)));
			if(GeometryUtils.intersectLine(q, p, p1, p2)) {
				return next;
			}

			next = getMesh().getNext(next);
		}

		throw new IllegalArgumentException("no second intersection edge fount");
	}

	// TODO: still required?
	default Optional<E> findIntersectionEdge(final double x1, final double y1, final V startVertex) {
		VPoint q = getMesh().toPoint(startVertex);
		VPoint p = new VPoint(x1, y1);

		E edge = getMesh().getEdge(startVertex);
		E candidate = getMesh().getPrev(edge);
		E next = getMesh().getTwin(getMesh().getNext(edge));

		do {
			candidate = getMesh().isBoundary(candidate) ? getMesh().getTwin(candidate) : candidate;
			if(isRightOf(x1, y1, candidate) && intersects(q, p, candidate)) {
				return Optional.of(candidate);
			}

			candidate = getMesh().getPrev(next);
			next = getMesh().getTwin(getMesh().getNext(next));
		} while (edge != next);

		return Optional.empty();
	}

	/**
	 * <p>Marching to the face which contains the point defined by (x1, y1) starting inside the startFace.
	 * This algorithm also works if there are convex polygon (holes) inside the triangulation.</p>
	 *
	 * <p>Assumption: (x1, y1) is contained in some face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x1        the x-coordinate of the ending point
	 * @param y1        the y-coordinate of the ending point
	 * @param startFace the face where the march start containing (x1,y1).
	 * @return returns the face containing (x1, y1)
	 */
	default F straightWalk2D(final double x1, final double y1, @NotNull final F startFace) {
		return straightWalk2D(x1, y1, startFace, e -> !isRightOf(x1, y1, e));
	}

	/**
	 * <p>Marches to the face which contains the point defined by (x1, y1) starting inside the <tt>startFace</tt>.
	 * Furthermore this method will gather all visited edges and requires O(n) time. However, if the face is close
	 * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
	 * inside the triangulation.</p>
	 *
	 * <p>Assumption: (x1, y1) is contained in some face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x1        the x-coordinate of the point at which the march will start
	 * @param y1        the y-coordinate of the point at which the march will start
	 * @param startFace the face where the march start containing (x1,y1).
	 * @return returns all visited edges in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
	 */
	default LinkedList<E> straightGatherWalk2D(final double x1, final double y1, @NotNull final F startFace) {
    	return straightGatherWalk2D(x1, y1, startFace, e -> !isRightOf(x1, y1, e));
	}

    default LinkedList<E> getIntersectingEdges(@NotNull final V vStart, @NotNull final V vEnd) {
		VPoint q = getMesh().toPoint(vStart);
		VPoint p = getMesh().toPoint(vEnd);
	    E firstEdge = null;

	    LinkedList<E> visitedEdges = new LinkedList<>();
	    for(E e : getMesh().getEdgeIt(vStart)) {
	    	E prev = getMesh().getPrev(e);
		    if(intersectsDirectional(q, p, prev)) {
			    firstEdge = getMesh().getTwin(prev);
			    visitedEdges.addLast(firstEdge);
		    }
	    }

	    Optional<E> optEdge = Optional.ofNullable(firstEdge);

	    // TODO: duplicated code
	    while(optEdge.isPresent()) {
		    E inEdge = optEdge.get();
		    optEdge = straightWalkNext(inEdge, q, p, e -> !isRightOf(vEnd.getX(), vEnd.getY(), e), visitedEdges);
		    if(optEdge.isPresent()) {
			    inEdge = optEdge.get();
			    visitedEdges.addLast(inEdge);

			    if(getMesh().isBorder(inEdge)) {
				    break;
			    }
			    else if(getMesh().isHole(inEdge)) {
			    	throw new IllegalArgumentException("reach a hole!");
			    }
		    }
	    }

	    return visitedEdges;
    }

	/**
	 * Marches from the midpoint of a face i.e. <tt>startFace</tt> in the direction (<tt>direction</tt>) until
	 * the stop-condition (<tt>stopCondition</tt>) is fulfilled. This requires O(n) worst case time, where n
	 * is the number of faces of the mesh.
	 *
	 * <p>Assumption: The stopCondition will be fulfilled at some point.</p>
	 *
	 * @param face                      the face at which the march / search starts
	 * @param direction                 the direction in which the march will go
	 * @param additionalStopCondition   the stop condition at which the march will stop
	 * @return all visited faces in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
	 */
	default LinkedList<E> straightWalk2DGatherDirectional(@NotNull final F face, @NotNull final VPoint direction, @NotNull final Predicate<E> additionalStopCondition) {
		VPoint q = getMesh().toMidpoint(face);
		assert getMesh().toTriangle(face).contains(q);

		Predicate<E> defaultStopCondion = e -> isRightOf(q.x, q.y, e);
		LinkedList<E> visitedFaces = straightGatherWalk2DDirectional(q, direction, face, defaultStopCondion.or(additionalStopCondition));

		return visitedFaces;
	}

	default F straightWalk2D(final double x1, final double y1, @NotNull final F startFace, @NotNull final Predicate<E> stopCondition) {
		return getMesh().getFace(straightGatherWalk2D(x1, y1, startFace, stopCondition).peekLast());
	}

	/**
	 * <p>Marches / walks along the line defined by q and p from q to p starting inside the startFace.
	 * Furthermore this method will gather all visited faces and requires O(n) time. However, if the face is close
	 * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
	 * inside the triangulation. A stop condition like (e to !isRightOf(x1, y1, e)) stops the walk if (x1, y1),
	 * will stop the walk if the point p = (x1, y1) is contained in the face.</p>
	 *
	 * Assumption:asd
	 * <ol>
	 *     <li>q is contained in the start face</li>
	 *     <li>the stop condition will be fulfilled at some point</li>
	 * </ol>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x1            the x-coordinate of the point at which the march will start
	 * @param y1            the y-coordinate of the point at which the march will start
	 * @param startFace     the face where the march start containing (x1,y1).
	 * @param stopCondition the stopCondition at which the march will stop.
	 * @return all visited faces in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
	 */
    default LinkedList<E> straightGatherWalk2D(final double x1, final double y1, @NotNull final F startFace, @NotNull final Predicate<E> stopCondition) {
        assert !getMesh().isBorder(startFace);

        // initialize
        F face = startFace;
        // for convex polygons we could also use: VPoint q = getMesh().toPolygon(startFace).getPolygonCentroid();
        VPoint q = getMesh().getTriangleMidPoint(startFace); // walk from q to p
        VPoint p = new VPoint(x1, y1);

        /*LinkedList<E> walkResult = straightGatherWalk2D(q, p, face, stopCondition);
	    List<F> faces = walkResult.stream().map(e -> getMesh().getFace(e)).collect(Collectors.toList());

		Runnable run = () -> {
	        Predicate<F> alertPredicate = f ->{
		        return faces.contains(f);
	        };

	        var meshPanel = new MeshPanel(getMesh(), alertPredicate, 800, 800);
	        meshPanel.display("Random walk");
        };

        new Thread(run).start();

        int count = 0;

	    while (count < 10) {
		    count++;
	    	try {
			    Thread.sleep(1000);
		    } catch (InterruptedException e) {
			    e.printStackTrace();
		    }
	    }*/

        return straightGatherWalk2D(q, p, face, stopCondition);
    }

	/**
	 * <p>Connects each (current) two consecutive border edge if they form an acute angle3D
	 * which smoothes the border of the mesh overall. This requires O(n) time, where n
	 * is the number of border edges.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 */
	default void smoothBorder() {
		for(E edge : getMesh().getEdges(getMesh().getBorder())) {
			if(getMesh().isBorder(edge)) {

				VPoint p = getMesh().toPoint(edge);
				VPoint q = getMesh().toPoint(getMesh().getNext(edge));
				VPoint r = getMesh().toPoint(getMesh().getPrev(edge));

				if(GeometryUtils.isCCW(r, p, q)) {
					double angle = GeometryUtils.angle(r, p, q);
					if(angle < 0.5*Math.PI) {
						createFaceAtBoundary(edge);
					}
				}
			}
		}
	}

	default void smoothHoles(@NotNull final IDistanceFunction distanceFunction, Predicate<V> isBoundary) {
		for(F hole : getMesh().getHoles()) {
			for(E edge : getMesh().getEdges(hole)) {

				/*
				 * to avoid duplicated smoothing
				 */
				if(getMesh().getFace(edge).equals(hole)) {
					V vp = getMesh().getVertex(edge);
					if(!isBoundary.test(vp)) {
						VPoint r = getMesh().toPoint(getMesh().getPrev(edge));
						VPoint p = getMesh().toPoint(edge);
						VPoint q = getMesh().toPoint(getMesh().getNext(edge));
						VPoint midPoint = new VLine(r, q).midPoint();

						if((distanceFunction.apply(p) + distanceFunction.apply(midPoint) < 0) && GeometryUtils.isCCW(r, p, q)) {
							double angle = GeometryUtils.angle(r, p, q);
							if(angle < 0.5*Math.PI) {
								createFaceAtBoundary(edge);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * <p>Connects each (current) two consecutive border edge if they form an acute angle3D
	 * which smoothes the border of the mesh overall. This requires O(n) time, where n
	 * is the number of border edges.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 */
	default void smoothHoles(@NotNull final IDistanceFunction distanceFunction) {
		smoothBorder(distanceFunction, v -> false);
	}

	default void smoothBoundary(@NotNull final IDistanceFunction distanceFunction, @NotNull final Predicate<V> predicate) {
		smoothBorder(distanceFunction, predicate);
		smoothHoles(distanceFunction, predicate);
	}

	default void smoothBoundary(@NotNull final IDistanceFunction distanceFunction) {
		smoothBorder(distanceFunction);
		smoothHoles(distanceFunction);
	}

	default void smoothBoundary() {
		smoothBorder(null);
		smoothHoles(null);
	}

	default void collapseBoundaryFaces(@NotNull final Predicate<F> collapsePredicate, @NotNull final Predicate<E> edgeCollapsePredicate, @NotNull final Consumer<V> action) {
		collapseBorderFaces(collapsePredicate, edgeCollapsePredicate, action);
		collapseHoleFaces(collapsePredicate, edgeCollapsePredicate, action);
	}

	default void collapseHoleFaces(@NotNull final Predicate<F> collapsePredicate, @NotNull final Predicate<E> edgeCollapsePredicate, @NotNull final Consumer<V> action) {
		for(F hole : getMesh().getHoles()) {
			for(E edge : getMesh().getEdges(hole)) {
				E twin = getMesh().getTwin(edge);
				F face = getMesh().getFace(twin);

				assert !getMesh().isBoundary(face);

				/*
				 * to avoid duplicated smoothing
				 */
				if(getMesh().getFace(edge).equals(hole) &&
						!getMesh().isAtBoundary(getMesh().getNext(twin)) && !getMesh().isAtBoundary(getMesh().getPrev(twin)) &&
						collapsePredicate.test(face)) {

					V vr = getMesh().getVertex(getMesh().getPrev(edge));
					V vp = getMesh().getVertex(getMesh().getNext(twin));
					V vq = getMesh().getVertex(edge);


					if(edgeCollapsePredicate.test(getMesh().getNext(twin))) {
						VPoint r = getMesh().toPoint(vr);
						VPoint q = getMesh().toPoint(vq);

						VPoint midPoint = new VLine(r, q).midPoint();
						removeFaceAtBoundary(face, hole,true);
						getMesh().setPoint(vp, midPoint);
						action.accept(vp);
					}
				}
			}
		}
	}

	default void collapseBorderFaces(@NotNull final Predicate<F> collapsePredicate, @NotNull final Predicate<E> edgeCollapsePredicate, @NotNull final Consumer<V> action) {
		for(E edge : getMesh().getEdges(getMesh().getBorder())) {
			E twin = getMesh().getTwin(edge);
			F face = getMesh().getFace(twin);

			assert !getMesh().isBoundary(face);

			/*
			 * to avoid duplicated smoothing
			 */
			if(getMesh().getFace(edge).equals(getMesh().getBorder()) &&
					!getMesh().isAtBoundary(getMesh().getNext(twin)) && !getMesh().isAtBoundary(getMesh().getPrev(twin)) &&
					collapsePredicate.test(face)) {

				V vr = getMesh().getVertex(getMesh().getPrev(edge));
				V vp = getMesh().getVertex(getMesh().getNext(twin));
				V vq = getMesh().getVertex(edge);


				if(edgeCollapsePredicate.test(getMesh().getNext(twin))) {
					VPoint r = getMesh().toPoint(vr);
					VPoint q = getMesh().toPoint(vq);

					VPoint midPoint = new VLine(r, q).midPoint();
					removeFaceAtBoundary(face, getMesh().getBorder(), true);
					getMesh().setPoint(vp, midPoint);
					action.accept(vp);
				}
			}
		}
	}

	default boolean isLargeAngle(@NotNull final E edge, double minAngle) {
		assert !getMesh().isBoundary(getMesh().getFace(edge));
		V vp = getMesh().getVertex(edge);
		V vq = getMesh().getVertex(getMesh().getNext(edge));
		V vr = getMesh().getVertex(getMesh().getPrev(edge));

		VPoint r = getMesh().toPoint(vr);
		VPoint p = getMesh().toPoint(vp);
		VPoint q = getMesh().toPoint(vq);

		if(GeometryUtils.isCCW(r, p, q)) {
			double angle = GeometryUtils.angle(r, p, q);
			return angle > minAngle;
		}

		return false;
	}

	default void smoothBorder(@Nullable final IDistanceFunction distanceFunction, @NotNull final Predicate<V> isBoundary) {
		for(E edge : getMesh().getEdges(getMesh().getBorder())) {

			/*
			 * to avoid duplicated smoothing
			 */
			if(getMesh().getFace(edge).equals(getMesh().getBorder())) {
				V vr = getMesh().getVertex(getMesh().getPrev(edge));
				V vp = getMesh().getVertex(edge);
				V vq = getMesh().getVertex(getMesh().getNext(edge));

				if(!isBoundary.test(vp)) {
					VPoint r = getMesh().toPoint(vr);
					VPoint p = getMesh().toPoint(vp);
					VPoint q = getMesh().toPoint(vq);


					VPoint midPoint = new VLine(r, q).midPoint();
					if(distanceFunction != null && (distanceFunction.apply(p) + distanceFunction.apply(midPoint) < 0)) {
						if(GeometryUtils.isCCW(r, p, q)) {
							double angle = GeometryUtils.angle(r, p, q);
							if(angle < 0.5*Math.PI) {
								//System.out.println(triangle);
								F newFace = createFaceAtBoundary(edge);
							}
						}
					}
				}
			}
		}
	}

	default void smoothBorder(@NotNull final IDistanceFunction distanceFunction) {
		for(E edge : getMesh().getEdges(getMesh().getBorder())) {

			/*
			 * to avoid duplicated smoothing
			 */
			if(getMesh().getFace(edge).equals(getMesh().getBorder())) {
				VPoint r = getMesh().toPoint(getMesh().getPrev(edge));
				VPoint p = getMesh().toPoint(edge);
				VPoint q = getMesh().toPoint(getMesh().getNext(edge));


				VPoint midPoint = new VLine(r, q).midPoint();

				VTriangle triangle = new VTriangle(r,p,q);
				if((distanceFunction.apply(p) + distanceFunction.apply(midPoint) < 0) && GeometryUtils.isCCW(r, p, q)) {
					double angle = GeometryUtils.angle(r, p, q);
					if(angle < 0.5*Math.PI) {
						//System.out.println(triangle);
						F newFace = createFaceAtBoundary(edge);
					}
				}
			}
		}
	}

    /**
     * <p>Walks along the line defined by q and p. The walking direction should be controlled by the stopCondition e.g.
     * (e to !isRightOf(x1, y1, e)) stops the walk if (x1, y1) is on the left side of each edge which is the case if the
     * point is inside the reached face. The walk starts at the startFace and continues in the direction of line defined
     * by q and p using the any edge which does not fulfill the stopCondition.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param q             start of the oriented-line
     * @param p             end of the oriented-line
     * @param startFace     at this face the walk starts
     * @param stopCondition fulfilling the stopCondition will stop the walk
     * @return the face containing p
     */
	default F straightWalk2D(final VPoint q, final VPoint p, final F startFace, final Predicate<E> stopCondition) {
        return getMesh().getFace(straightGatherWalk2D(q, p, startFace, stopCondition).peekLast());
	}

	/**
	 * <p>This method is called whenever the special case appear during a 2D-walk which is the following:
	 * There is at least one point v of the face which lies on the line (q,p) and p is not contained in the face.</p>
	 *
	 * In this case we go around all the neighbouring faces of v searching for an intersection edge which is not equals to in-edge.
	 * It might be the case that the line (q,p) goes through two points of a neighbouring face. Therefore, we test first for
	 * intersection of (p1,p3) where p1,p2,p3 are consecutive points of the ring around the v. If (q,p) intersects (p1,p3) it has to
	 * intersect (p1,p2) or (p2,p3), otherwise (q,p) goes through p2. If so the method returns (p1,p2) or (p2,p3). This is more expensive
	 * than the general case i.e. O(d) where d is the degree of v. However this should not happen often in a general triangulation and
	 * can only occur more often in very degenerated triangulations.
	 *
	 * @param inEdge    the edge at which we enter the face i.e. this edge intersects (q,p)
	 * @param q         the first point of the directed line (q,p)
	 * @param p         the second point of the directed line (q,p)
	 * @return the next in-edge intersecting (q,p) which is unequal to <tt>inEdge</tt> for the special case
	 */
	default E straightWalkSpecialCase(@NotNull final E inEdge,
	                                  @NotNull final VPoint q,
	                                  @NotNull final VPoint p) {

		/**
		 * (1) get the vertex v which intersects with (q,p)
		 */
		V vertex = getSpecialVertex(inEdge, q, p);
		E nextOutEdge = null;
		for(E e : getMesh().getEdgeIt(vertex)) {
			E prev = getMesh().getPrev(e);          // v1 -> v3
			E next = getMesh().getNext(e);          // vertex -> v1
			E twin = getMesh().getTwin(e);          // vertex -> v3
			E twinNext = getMesh().getNext(twin);   // v3 -> v2

			V v1 = getMesh().getVertex(next);
			V v2 = getMesh().getVertex(twinNext);
			V v3 = getMesh().getVertex(twin);

			if(contains(p.getX(), p.getY(), v2, vertex, v3)) {
				nextOutEdge = twinNext;
			}

			if(contains(p.getX(), p.getY(), vertex, v1, v3)) {
				nextOutEdge = prev;
			}

			if(nextOutEdge == null && GeometryUtils.isRightOf(v1.getX(), v1.getY(), v2.getX(), v2.getY(), p.getX(), p.getY()) && intersects(q, p, v1, v2)) {
				if(!inEdge.equals(prev) && intersects(q, p, prev)) {
					nextOutEdge = prev;
				}
				else if(!inEdge.equals(twinNext) && intersects(q, p, twinNext)) {
					nextOutEdge = twinNext;
				}
				else {
					nextOutEdge = prev;
				}
			}

			if(nextOutEdge != null) {
				break;
			}
		}

		if(nextOutEdge == null) {
			throw new IllegalArgumentException("this should never happen!");
		}

		return nextOutEdge;
	}

	/**
	 * Returns the vertex of the face of the <tt>inEdge</tt> which is closest to the line segment
	 * (q, p).
	 *
	 * @param inEdge
	 * @param q
	 * @param p
	 * @return
	 */
	default V getSpecialVertex(@NotNull final E inEdge,
	                           @NotNull final VPoint q,
	                           @NotNull final VPoint p) {

		for(V v : getMesh().getVertexIt(getMesh().getFace(inEdge))) {
			if(GeometryUtils.distanceToLineSegment(q, p, v) < GeometryUtils.DOUBLE_EPS) {
				return v;
			}
		}

		throw new IllegalArgumentException("no intersection point found " + q + " -> " + p);
	}

	//Ray casting
	/**
	 * This method returns the edge of a face (defined by its half-edge inEdge) which
	 * 1) intersects the line (q,p) and
	 * 2) its intersection point is the closest one with respect to p
	 *
	 * This is computational expensive because one iterates over all edges of the face (potentially a hole or the border).
	 * But it is a robust method to walk through a non-convex hole or border!
	 *
	 * @param inEdge
	 * @param q
	 * @param p
	 * @param stopCondition
	 * @param visitedEdges
	 * @return
	 */
	default E rayCastingPolygon(@NotNull final E inEdge,
	                            @NotNull final VPoint q,
	                            @NotNull final VPoint p,
	                            @NotNull final Predicate<E> stopCondition) {

		E outEdge = null;
		E outIfInside = null;
		F face = getMesh().getFace(inEdge);

		// TODO: this seems to be expensive
		int count = 0;
		double distance = Double.MAX_VALUE;
		for(E e : getMesh().getEdgeIt(inEdge)) {
			if(intersectsDirectional(p, q, e)) {
				count++;
				//if((!stopCondition.test(e) && !getMesh().isBorder(face)) || (stopCondition.test(e) && getMesh().isBorder(face))) {
				V v1 = getMesh().getVertex(e);
				V v2 = getMesh().getTwinVertex(e);
				VPoint iPoint = GeometryUtils.intersectionPoint(q.getX(), q.getY(), p.getX(), p.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());
				double dist = p.distance(iPoint);
				if(dist < distance) {
					outEdge = e;
					distance = dist;
				}
				//} else {
				//	outIfInside = e;
				//}
			}
		}

		boolean isInside = count % 2 == 1;

		return (isInside && !getMesh().isBorder(face) || !isInside && getMesh().isBorder(face)) ? null : outEdge;
	}

	default E walkAroundBoundaryStraight(@NotNull final E inEdge,
	                            @NotNull final VPoint q,
	                            @NotNull final VPoint p,
	                            @NotNull final Predicate<E> stopCondition) {
		E outEdge = null;
		E outIfInside = null;
		F face = getMesh().getFace(inEdge);

		V v1 = getMesh().getVertex(inEdge);
		V v2 = getMesh().getTwinVertex(inEdge);

		double angle = GeometryUtils.angle2D(p.x-q.x, p.y-q.y,
				getMesh().getX(v1) - getMesh().getX(v2), getMesh().getY(v1) - getMesh().getY(v2));

		boolean walkForward = angle <= Math.PI * 0.5;

		Iterable<E> iterable = walkForward ? getMesh().getEdgeIt(inEdge) : getMesh().getEdgeItReverse(inEdge);
		for(E e : iterable) {
			if(!e.equals(inEdge) && intersectsDirectional(p, q, e)) {
				outEdge = e;
				break;
			}
		}

		if(outEdge == null) {
			return outEdge;
		}

		if(getMesh().isBoundary(face) && isLeftOf(p.getX(), p.getY(), outEdge)) {
			return rayCastingPolygon(inEdge, q, p, stopCondition);
		} else {
			return outEdge;
		}
	}

	/**
	 * Walks one step i.e. from a face to immediate / neighbouring next face along the line defined by q and p
	 * from q to p. This is done be walking from an in-edge through the face to the out-edge. Both the in-edge and
	 * the out-edge intersects the line (q,p). Furthermore this method will gather the visited edges by placing them
	 * into the list of visited edges. There are different cases with special cases which make the code complicated:
	 * <ol>
	 *     <li>general case (1):    the line (q,p) intersects two half-edges.
	 *                              In this case the algorithm walks across the correct line by the definition of the direction (i.e. towards p)</li>
	 *     <li>special case (2.1):  the line (q,p) goes through a point of the face of the in-edge and therefore the is no out-line intersecting (q,p)
	 *                              but the face contains p. In this case the walk is finished.</li>
	 *     <li>special case (2.2):  the line (q,p) goes through a point v of the face of the in-edge and therefore the is no out-line intersecting (q,p)
	 * 	                            but the face does not contain p. In this case we go around all the neighbouring faces of v searching for an intersection
	 * 	                            edge which is not equals to in-edge see {@see ITriangulation#straightWalkSpecialCase}. This is more expensive than the general case i.e. O(d) where d is the degree of v.
	 * 	                            However this should not happen often in a general triangulation and can only accure more often in very degenerated triangulations.</li>
	 * </ol>
	 *
	 * <p>Assumption: inEdge intersects (q,p) and it is not the next out-edge and the stop-condition makes sense.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param q             start point of the march / walk
	 * @param p             end point of the march / walk
	 * @param inEdge        start face of the walk
	 * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
	 * @param visitedEdges  a list which will be filled with the visited faces in order in which they are visited (first visited = first in)
	 * @return all visited edges in a first visited first in ordered queue, i.e. {@link LinkedList}.
	 */
	default Optional<E> straightWalkNext(
			@NotNull final E inEdge,
			@NotNull final VPoint q,
			@NotNull final VPoint p,
			@NotNull final Predicate<E> stopCondition,
			@Nullable final LinkedList<E> visitedEdges) {
		E outEdge = null;
		F face = getMesh().getFace(inEdge);

		/**
		 * Special case: the face is a hole or the border!
		 */
		if(getMesh().isBoundary(face)) {
			//return Optional.empty();

			V v1 = getMesh().getVertex(inEdge);
			V v2 = getMesh().getTwinVertex(inEdge);
			VPoint iPoint1 = GeometryUtils.intersectionPoint(q.getX(), q.getY(), p.getX(), p.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());

			outEdge = walkAroundBoundaryStraight(inEdge, q, p, stopCondition);
			//outEdge = rayCastingPolygon(inEdge, q, p, stopCondition);
			// the point outside
			if(outEdge == null) {
				return Optional.empty();
			}

			v1 = getMesh().getVertex(outEdge);
			v2 = getMesh().getTwinVertex(outEdge);
			VPoint iPoint2 = GeometryUtils.intersectionPoint(q.getX(), q.getY(), p.getX(), p.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());

			// we did no progress towards p => walking around does not work cause the boundary is not convex, therefore we use the expensive method
			if(iPoint1.distanceSq(p) <= iPoint2.distanceSq(p)) {
				// TODO this is too expensive!
				outEdge = rayCastingPolygon(inEdge, q, p, stopCondition);
			}

			// the point outside
			if(outEdge == null) {
				return Optional.empty();
			} else {
				return Optional.of(getMesh().getTwin(outEdge));
			}
		} else {
			/**
			 * Get the half-edges e which intersects (q, p).
			 */
			for(E e : getMesh().getEdgeIt(getMesh().getNext(inEdge))) {
				if(!e.equals(inEdge) && intersects(q, p, e)) {
					outEdge = e;
					break;
				}
			}
		}

		/**
		 * General case (1): The line defined by (q,p) intersects 2 edges of the convex polygon.
		 */
		if(outEdge != null) {
			//log.debug("straight walk: general case");
			boolean stop = stopCondition.test(outEdge);
			if(!stopCondition.test(outEdge)) {
				return Optional.of(getMesh().getTwin(outEdge));
			}
			else {
				return Optional.empty();
			}
		}
		/**
		 * Special case (2): There is one or two points of the polygon which are collinear with the line defined by (q,p).
		 */
		else {
			/**
			 * Good case (2.1) There are two collinear points but the face contains p => p lies on an edge of the face.
			 */
			if(contains(p.getX(), p.getY(), face) || getMesh().isCloseTo(face, p.getX(), p.getY())) {
				log.debug("no intersection line but contained or very close.");
				return Optional.empty();
			}
			/**
			 * Bad case (2.2): This which should not happen in general: q, the exit point v and p are collinear, therefore there is no exit intersection line!
			 * We continue the search with the face which centroid is closest to p! v has to be the closest p as well.
			 */
			else {
				log.debug("straight walk: no exit edge found due to collinear exit point.");

				/**
				 * Get the face with the centroid closest to p and which was not visited already.
				 */
				E nextOutEdge = straightWalkSpecialCase(inEdge, q, p);
				if(!stopCondition.test(nextOutEdge)) {
					return Optional.of(getMesh().getTwin(nextOutEdge));
				}
				else {
					if(visitedEdges != null) {
						visitedEdges.add(nextOutEdge);
					}
					return Optional.empty();
				}

				/*if(!closestFace.isPresent()) {
					SimpleTriCanvas canvas = SimpleTriCanvas.simpleCanvas(getMesh());
					getMesh().streamFaces(v).forEach(f -> canvas.getColorFunctions().overwriteFillColor(f, Color.MAGENTA));
					DebugGui.setDebugOn(true);
					if(DebugGui.isDebugOn()) {
						canvas.addGuiDecorator(graphics -> {
							Graphics2D graphics2D = (Graphics2D)graphics;
							graphics2D.setColor(Color.GREEN);
							graphics2D.setStroke(new BasicStroke(0.05f));
							graphics2D.draw(new VLine(q, p));
							log.info("p: " + p);
							graphics2D.fill(new VCircle(q, 0.05f));
						});
						DebugGui.showAndWait(canvas);
					}
				}*/

				/*graphics2D.setStroke(new BasicStroke(0.05f));
		    logger.info("p: " + p);
		    graphics2D.draw(new VLine(p, p.add(direction1.scalarMultiply(10))));
		    graphics2D.setColor(Color.BLUE);
		    graphics2D.draw(new VLine(p, p.add(direction2.scalarMultiply(10))));*/

				//assert closestFace.isPresent() : visitedFaces.size();
				//return Optional.of(getMesh().getTwin(outEdge));
			}

		}
	}

	/**
	 * <p>Marches / walks along the line defined by q and p from q to p starting inside the startFace.
	 * Furthermore this method will gather all visited edges and requires O(n) time. However, if the face is close
	 * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
	 * inside the triangulation. A stop condition like (e to !isRightOf(x1, y1, e)) stops the walk if (x1, y1),
	 * will stop the walk if the point p = (x1, y1) is contained in the face.
	 * The method goes from one face to the next by calling {@link ITriConnectivity#straightWalkNext(E, VPoint, VPoint, Predicate, LinkedList)}
	 * but adds the resulting edge to the list of visited edges and adds some logging to debug the walks / marches.</p>
	 *
	 * <p>Assumption: q is contained in the start face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param q             start point of the march / walk
	 * @param p             end point of the march / walk
	 * @param startFace     start face of the walk
	 * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
	 * @return all visited edges in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
	 */
	default LinkedList<E> straightGatherWalk2D(final VPoint q, final VPoint p, final F startFace, final Predicate<E> stopCondition) {
		return straightGatherWalk2D(q, p, startFace, stopCondition, false, false);
	}

	/**
	 * <p>Marches / walks from q (which is contained in <tt>startFace</tt>) in direction <tt>direction</tt>.
	 * Furthermore this method will gather all visited edges and requires O(n) time where n is the number of
	 * visited edges and can be as large as the number of triangles. However, if the face is close
	 * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
	 * inside the triangulation. A stop condition like (e to !isRightOf(p.x, p.y, e)) stops the walk if
	 * the point p = (x, y) is contained in the face. The method goes from one face to the next by calling
	 * {@link ITriConnectivity#straightWalkNext(E, VPoint, VPoint, Predicate, LinkedList)}.</p>
	 *
	 * <p>Assumption: q is contained in the start face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param q             start point of the march / walk
	 * @param direction     direction of the walk
	 * @param startFace     start face of the walk
	 * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
	 * @return all visited edges in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
	 */
	default LinkedList<E> straightGatherWalk2DDirectional(final VPoint q, final VPoint direction, final F startFace, final Predicate<E> stopCondition) {
		return straightGatherWalk2D(q, direction, startFace, stopCondition, true, false);
	}

	/**
	 * <p>Marches / walks along the line defined by q and p from q to p starting inside the startFace, i.e. q has to be
	 * contained in the <tt>startFace</tt>. Furthermore, this method will gather all visited edges and requires O(n) time
	 * where n is the number of visited edges which can be as large as the number of triangles. However, if the face is close
	 * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
	 * inside the triangulation. A stop condition like (e to !isRightOf(p.x, p.y, e)) stops the walk if the point p = (x, y)
	 * is contained in the face. The method goes from one face to the next by calling
	 * {@link ITriConnectivity#straightWalkNext(E, VPoint, VPoint, Predicate, LinkedList)}.</p>
	 *
	 * <p>Assumption: q is contained in the start face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param q             start point of the march / walk
	 * @param pDirection    the point we are walking to or the direction towards we are walking to which is decided by <tt>directional</tt>
	 * @param startFace     start face of the walk
	 * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
	 * @param directional   if true we walk in the direction of <tt>pDirection</tt> otherwise we walk to the face containing <tt>pDirection</tt>
	 * @param gather
	 * @return all visited edges in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
	 */
	default LinkedList<E> straightGatherWalk2D(
			@NotNull final VPoint q,
			@NotNull final VPoint pDirection,
			@NotNull final F startFace,
			@NotNull final Predicate<E> stopCondition,
			final boolean directional,
			final boolean gather) {
		LinkedList<E> visitedEdges = new LinkedList<>();

		assert contains(q.getX(), q.getY(), startFace);

		/**
		 * (1) find the initial in-edge.
		 */
		E inEdge = null;

		/**
		 * Find the half-edge which intersects the line-segment (q, q+pDirection) but not the half-line (q, q+pDirection).
		 * This will be the first in-edge and the other half-edge intersecting (q, q+pDirection) will be the out-edge.
		 */
		VPoint p;
		if(directional) {
			p = q.add(pDirection);
			for(E e : getMesh().getEdgeIt(startFace)) {
				// line intersection

				if(intersects(q, p, e) && !intersectsDirectional(q, p, e)) {
					inEdge = e;
					break;
				}
			}

			/**
			 * this might happen if the line intersects a point, in this case both neighbouring edges are feasible
			 */
			if(inEdge == null) {
				inEdge = getMesh().streamEdges(startFace).filter(e -> !intersectsDirectional(q, p, e)).findAny().get();
			}

		}
		/**
		 * Find the half-edge which intersects the line-segment (q, pDirection) and has pDirection on its left side.
		 * This will be the first in-edge and the other half-edge intersecting (q, q+pDirection) will be the out-edge.
		 */
		else {
			p = pDirection;
			// if this is true we are already done
			if(contains(pDirection.getX(), pDirection.getY(), startFace)) {
				if(!gather) {
					visitedEdges.clear();
				}
				visitedEdges.add(getMesh().getEdge(startFace));
				return visitedEdges;
			}
			// find the entering edge
			for(E e : getMesh().getEdgeIt(startFace)) {
				// line intersection
				if(intersects(q, pDirection, e) && isLeftOfRobust(p.getX(), p.getY(), e)) {
					inEdge = e;
					break;
				}
			}

			/**
			 * this might happen if the line intersects a point, in this case both neighbouring edges are feasible
			 */
			if(inEdge == null) {
				Optional<E> optEdge = getMesh().streamEdges(startFace).filter(e -> isLeftOf(p.getX(), p.getY(), e)).findAny();
				inEdge = optEdge.get();
			}
		}

		if(inEdge == null) {
			throw new IllegalArgumentException("did not find any edge.");
		}

		if(!gather) {
			visitedEdges.clear();
		}
		visitedEdges.addLast(inEdge);


		Optional<E> optEdge;

		/**
		 * (2) find all other in-edges.
		 */
		do {

			/*if(visitedEdges.size() > 400) {
				System.out.println("startFace: " + startFace);
				System.out.println(pDirection);
				System.out.println(getMesh().toTriangle(startFace).midPoint());

			    List<F> faces = visitedEdges.stream().map(e -> getMesh().getFace(e)).collect(Collectors.toList());

				Runnable run = () -> {
			        Predicate<F> alertPredicate = f ->{
				        return faces.contains(f);
			        };

			        var meshPanel = new MeshPanel(getMesh(), alertPredicate, 1500, 1500);
			        meshPanel.display("Random walk");
		        };

		        new Thread(run).start();

		        int count = 0;

			    while (count < 10000) {
				    count++;
			        try {
					    Thread.sleep(1000);
				    } catch (InterruptedException e) {
					    e.printStackTrace();
				    }
			    }
			}*/

        	/*if (DebugGui.isDebugOn() && getMesh().getFacesWithHoles().size() >= 5) {
		        DebugGui.showAndWait(WalkCanvas.getDefault(
				        getMesh(),
				        q,
				        p,
				        startFace,
				        startEdge,
				        visitedFaces));
	        }*/



//			log.debug(getMesh().toPath(face));
			// TODO: this might be slow
			if(directional) {
				optEdge = straightWalkNext(inEdge, q, p, stopCondition, visitedEdges);
			}
			else {
				optEdge = straightWalkNext(inEdge, q, p, stopCondition, visitedEdges);
			}


			if(!optEdge.isPresent()) {
				//log.info("expensive fix");
				//optFace = straightWalkNext(face, q, p, stopCondition);
			}
			else {
				//log.info("fast");
			}

			if(optEdge.isPresent()) {
				inEdge = optEdge.get();
				if(!gather) {
					visitedEdges.clear();
				}
				visitedEdges.addLast(inEdge);

				// special case (1): hitting the border i.e. outer boundary
				// special case (2): hitting a hole
				if(getMesh().isBorder(inEdge)) {
					//log.debug("walked towards the border!");
					// return the border
					// log.debug(getMesh().toPath(face));
					//break;
				}
				else if(getMesh().isHole(inEdge)) {
					//log.debug("walked towards a hole!");
					// just go on with the normal straight walk which works for CONVEX polygons!
				}
			}
		} while (optEdge.isPresent());

		/*if (!contains(p.getX(), p.getY(), getMesh().getFace(visitedEdges.peekLast()))) {
			java.util.List<F> visitedFaces = visitedEdges.stream().map(e -> getMesh().getFace(e)).collect(Collectors.toList());
			Function<F, Color> colorFunction = f -> visitedFaces.contains(f) ? Color.GREEN : Color.WHITE;
			System.out.println(TexGraphGenerator.toTikz(getMesh(), colorFunction, 1.0f, new VLine(q, pDirection)));

		}*/

		//log.debug("visited faces for location: " + visitedEdges.size());
		return visitedEdges;
    }

	/**
	 * <p>Marches / walks to the face which contains the point defined by (x1, y1) starting the walk
	 * inside the start face (<tt>startFace</tt>). The algorithm is an implementation of the
	 * Probabilistic / Random walk which is faster in practice, see Walking in a Triangulation by devillers-2001.
	 * This algorithm does NOT works if there are convex polygon (holes) inside the triangulation.</p>
	 *
	 * Assumption:
	 * <ol>
	 *     <li>(x1, y1) is contained in some face / triangle</li>
	 *     <li>the mesh / triangulation does not contain holes</li>
	 * </ol>
	 *
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x1        the x-coordinate of the ending point
	 * @param y1        the y-coordinate of the ending point
	 * @param startFace the face where the march start containing (x1,y1).
	 * @return the face containing the point (x1, y1)
	 */
	default F marchRandom2D(final double x1, final double y1, @NotNull final F startFace) {
		assert getMesh().getHoles().size() == 0;

		boolean first = true;
		F face = startFace;
		F prevFace = null;
		int count = 0;

		while (true) {

			if(getMesh().isBoundary(face)) {
				if(contains(x1, y1, face)) {
					return face;
				}
				else {
					throw new IllegalArgumentException("marchRandom2D can not walk through holes.");
				}
			}

			count++;
			boolean goLeft = random.nextBoolean();
			//boolean goLeft = true;

			E e1 = getMesh().getEdge(face);
			E e2 = getMesh().getNext(e1);
			E e3 = getMesh().getNext(e2);

			V v1 = getMesh().getVertex(e1);
			V v2 = getMesh().getVertex(e2);
			V v3 = getMesh().getVertex(e3);

			// loop unrolling for efficiency!
			if(first) {
				first = false;
				prevFace = face;

				if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
					face = getMesh().getTwinFace(e1);
					continue;
				}

				if (GeometryUtils.isRightOf(v1, v2, x1, y1)) {
					face = getMesh().getTwinFace(e2);
					continue;
				}

				if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
					face = getMesh().getTwinFace(e3);
					continue;
				}
			} else if(goLeft) {
				if(prevFace == getMesh().getTwinFace(e1)) {
					prevFace = face;

					if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
						face = getMesh().getTwinFace(e3);
						continue;
					}

					if(GeometryUtils.isRightOf(v1, v2, x1, y1)) {
						face = getMesh().getTwinFace(e2);
						continue;
					}

				}
				else if(prevFace == getMesh().getTwinFace(e2)) {
					prevFace = face;

					if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
						face = getMesh().getTwinFace(e1);
						continue;
					}

					if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
						face = getMesh().getTwinFace(e3);
						continue;
					}

				}
				else {
					prevFace = face;

					if(GeometryUtils.isRightOf(v1, v2, x1, y1)) {
						face = getMesh().getTwinFace(e2);
						continue;
					}

					if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
						face = getMesh().getTwinFace(e1);
						continue;
					}

				}
			}
			else {
				if(prevFace == getMesh().getTwinFace(e1)) {
					prevFace = face;

					if(GeometryUtils.isRightOf(v1, v2, x1, y1)) {
						face = getMesh().getTwinFace(e2);
						continue;
					}

					if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
						face = getMesh().getTwinFace(e3);
						continue;
					}

				}
				else if(prevFace == getMesh().getTwinFace(e2)) {
					prevFace = face;

					if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
						face = getMesh().getTwinFace(e3);
						continue;
					}

					if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
						face = getMesh().getTwinFace(e1);
						continue;
					}

				}
				else {
					prevFace = face;

					if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
						face = getMesh().getTwinFace(e1);
						continue;
					}

					if(GeometryUtils.isRightOf(v1, v2, x1, y1)) {
						face = getMesh().getTwinFace(e2);
						continue;
					}

				}
			}
			//log.info("#traversed triangles = " + count);
			return face;
		}
	}

	default boolean contains(final double x, final double y, @NotNull final F face) {
		if(!getMesh().isBoundary(face)) {
			//return getMesh().toImmutableTriangle(face).contains(x, y);
			E e1 = getMesh().getEdge(face);
			V v1 = getMesh().getVertex(e1);
			V v3 = getMesh().getTwinVertex(e1);
			V v2 = getMesh().getVertex(getMesh().getNext(e1));

			double x1 = getMesh().getX(v1);
			double y1 = getMesh().getY(v1);
			double x2 = getMesh().getX(v2);
			double y2 = getMesh().getY(v2);
			double x3 = getMesh().getX(v3);
			double y3 = getMesh().getY(v3);

			return !GeometryUtils.isRightOf(x1, y1, x2, y2, x, y) &&
					!GeometryUtils.isRightOf(x2, y2, x3, y3, x, y) &&
					!GeometryUtils.isRightOf(x3, y3, x1, y1, x, y);
		} else {
			return IPolyConnectivity.super.contains(x, y, face);
		}
	}


	/*default V locateNearestNeighbour(double x1, double y1, F face) {
		assert isInsideCircumscribedCycle(face, x1, y1);

		V nn = getMesh().getNearestPoint(face, x1, y1);
		E edge = getMesh().getEdge(face);


		nn = lookUpNearestNeighbour(x1, y1, nn, getMesh().getTwin(edge));
		nn = lookUpNearestNeighbour(x1, y1, nn, getMesh().getTwin(getMesh().getNext(edge)));
		nn = lookUpNearestNeighbour(x1, y1, nn, getMesh().getTwin(getMesh().getNext(edge)));

		return nn;
	}

	default V locateNearestNeighbour(P point, F startFace) {
		return locateNearestNeighbour(point.getX(), point.getY(), startFace);
	}

	default V locateNearestNeighbour(P point) {
		return locateNearestNeighbour(point.getX(), point.getY(), getMesh().getFace());
	}

	default V locateNearestNeighbour(double x1, double y1) {
		return locateNearestNeighbour(x1, y1, getMesh().getFace());
	}

	default V lookUpNearestNeighbour(double x1, double y1, V nn, E edge) {
		F face = getMesh().getFace(edge);

		if(isInsideCircumscribedCycle(face, x1, y1)) {
			V v = getMesh().getVertex(edge);

			if(v.distance(x1, y1) < nn.distance(x1, y1)) {
				nn = v;
			}

			nn = lookUpNearestNeighbour(x1, y1, nn, getMesh().getTwin(getMesh().getNext(edge)));
			nn = lookUpNearestNeighbour(x1, y1, nn, getMesh().getTwin(getMesh().getPrev(edge)));
		}

		return nn;
	}*/

	/**
	 * <p>Tests if the point p = (x1, y1) is inside the circumscribed cycle defined by the triangle of the face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param face  the face
	 * @param x1    x-coordinate of the point
	 * @param y1    y-coordinate of the point
	 * @return true if the point p = (x1, y1) is inside the circumscribed cycle defined by the triangle of the face, false otherwise.
	 */
	default boolean isInsideCircumscribedCycle(@NotNull final F face, final double x1, final double y1) {
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);

		V v1 = getMesh().getVertex(e1);
		V v2 = getMesh().getVertex(e2);
		V v3 = getMesh().getVertex(e3);

		return GeometryUtils.isInsideCircle(v1, v2, v3, x1, y1);
	}

	/*default boolean contains(F face, double x, double y) {
		if(isCloseTo(face, x, y)) {
			return true;
		}

		// in this case the face might be a polygon
		if(getMesh().isBoundary(face)) {
			return getMesh().toPolygon(face).contains(x, y);
		}
		else {
			E h1 = getMesh().getEdge(face);
			E h2 = getMesh().getNext(h1);
			E h3 = getMesh().getNext(h2);
			return Utils.triangleContains(getMesh().getPoint(h1), getMesh().getPoint(h2), getMesh().getPoint(h3), new VPoint(x, y));
		}
	}*/

	//Optional<F> marchLocate2DLFC(double x, double y, F startFace);


	/*default V getMaxDistanceVertex(F face, double x, double y) {
		List<V> vertices = getMesh().getVertices(face);

		//assert vertices.size() == 3;

		V result = vertices.get(0);
		result = result.distance(x, y) < vertices.get(1).distance(x, y) ? vertices.get(1) : result;
		result = result.distance(x, y) < vertices.get(2).distance(x, y) ? vertices.get(2) : result;
		return result;
	}*/

	/*default V getMinDistanceVertex(F face, double x, double y) {
		List<V> vertices = getMesh().getVertices(face);

		//assert vertices.size() == 3;

		V result = vertices.get(0);
		result = result.distance(x, y) > vertices.get(1).distance(x, y) ? vertices.get(1) : result;
		result = result.distance(x, y) > vertices.get(2).distance(x, y) ? vertices.get(2) : result;
		return result;
	}*/

	/**
	 * <p>Returns the dimension of the triConnectivity.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @return the dimension of the triConnectivity
	 */
	default int getDimension() {
		return getMesh().getNumberOfVertices() - 2;
	}

	/**
	 * Returns the closest half-edge (with respect to (x,y)) of a face containing (x,y) if there is any face that contains p, otherwise empty().
	 * Three cases are possible:
	 * <ol>
	 *     <li>p is in the interior of the face</li>
	 *     <li>p lies on the edge which will be returned</li>
	 *     <li>p is a vertex of the mesh</li>
	 * </ol>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x x-coordinate of the point p
	 * @param y y-coordinate of the point p
	 * @return the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
	 */
	default Optional<E> getClosestEdge(final double x, final double y) {
		Optional<F> optFace = locate(x, y);

		if(optFace.isPresent()) {
			return Optional.of(getMesh().closestEdge(optFace.get(), x, y));
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Returns the closest half-edge (with respect to (x,y)) of a face containing (x,y) if there is any face that contains p, otherwise empty().
	 * Three cases are possible:
	 * <ol>
	 *     <li>p is in the interior of the face</li>
	 *     <li>p lies on the edge which will be returned</li>
	 *     <li>p is a vertex of the mesh</li>
	 * </ol>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x         x-coordinate of the point p
	 * @param y         y-coordinate of the point p
	 * @param startFace the face from which the walk / march / search will start
	 * @return the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
	 */
	default Optional<E> getClosestEdge(final double x, final double y, final F startFace) {
		Optional<F> optFace = locateMarch(x, y, startFace);

		if(optFace.isPresent()) {
			return Optional.of(getMesh().closestEdge(optFace.get(), x, y));
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Returns the closest vertex (with respect to (x,y)) of a face containing (x,y) if there is any face that contains p, otherwise empty().
	 * Three cases are possible:
	 * <ol>
	 *     <li>p is in the interior of the face</li>
	 *     <li>p lies on the edge which will be returned</li>
	 *     <li>p is a vertex of the mesh</li>
	 * </ol>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x         x-coordinate of the point p
	 * @param y         y-coordinate of the point p
	 * @return the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
	 */
	default Optional<V> getClosestVertex(final double x, final double y) {
		Optional<F> optFace = locate(x, y);

		if(optFace.isPresent()) {
			return Optional.of(getMesh().closestVertex(optFace.get(), x, y));
		}
		else {
			return Optional.empty();
		}
	}


	/**
	 * Returns the closest vertex (with respect to (x,y)) of a face containing (x,y) if there is any face that contains p, otherwise empty().
	 * Three cases are possible:
	 * <ol>
	 *     <li>p is in the interior of the face</li>
	 *     <li>p lies on the edge which will be returned</li>
	 *     <li>p is a vertex of the mesh</li>
	 * </ol>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x         x-coordinate of the point p
	 * @param y         y-coordinate of the point p
	 * @param startFace the face from which the walk / march / search will start
	 * @return the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
	 */
	default Optional<V> getClosestVertex(final double x, final double y, final F startFace) {
		Optional<F> optFace = locateMarch(x, y, startFace);

		if(optFace.isPresent()) {
			return Optional.of(getMesh().closestVertex(optFace.get(), x, y));
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * <p>Returns true if and only if the mesh of this ITriConnectivity is a valid Triangulation.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @return true if this the mesh of this ITriConnectivity is a valid Triangulation, false otherwise
	 */
	default boolean isValid() {
		Predicate<F> orientationPredicate = f -> {
			E edge = getMesh().getEdge(f);
			IPoint p1 = getMesh().getPoint(getMesh().getPrev(edge));
			IPoint p2 = getMesh().getPoint(edge);
			IPoint p3 = getMesh().getPoint(getMesh().getNext(edge));
			boolean valid = GeometryUtils.isLeftOf(p1, p2, p3);
			if (!valid) {
				log.info(p1 + ", " + p2 + ", " + p3);
			}
			return valid;
		};

		//log.debug(getMesh().streamFaces().filter(f -> !getMesh().isDestroyed(f)).filter(f -> !getMesh().isBoundary(f)).filter(e -> !orientationPredicate.test(e)).count() + " invalid triangles");
		return getMesh().streamFaces().filter(f -> !getMesh().isDestroyed(f)).allMatch(orientationPredicate);
	}

	/**
	 * <p>Returns true if and only if the face is a valid triangle.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param face the face which will be tested
	 *
	 * @return true if this the mesh of the face is a valid triangle, false otherwise
	 */
	default boolean isValid(@NotNull final F face) {
		Predicate<F> orientationPredicate = f -> {
			E edge = getMesh().getEdge(f);
			IPoint p1 = getMesh().getPoint(getMesh().getPrev(edge));
			IPoint p2 = getMesh().getPoint(edge);
			IPoint p3 = getMesh().getPoint(getMesh().getNext(edge));
			return GeometryUtils.isLeftOf(p1, p2, p3);
		};

		return !getMesh().isBoundary(face) && orientationPredicate.test(face);
	}

	default IPoint[] getPoints(@NotNull final E edge) {
		final IPoint[] points = new IPoint[3];
		points[0] = getMesh().getPoint(edge);
		points[1] = getMesh().getPoint(getMesh().getNext(edge));
		points[2] = getMesh().getPoint(getMesh().getPrev(edge));
		return points;
	}

	default IPoint[] getPoints(F face) {
		return getPoints(getMesh().getEdge(face));
	}

	default void getTriPoints(@NotNull final F face, double[] x, double[] y, double[] z, @NotNull final IVertexContainerDouble<V, E, F> distances){
		assert x.length == y.length && y.length == z.length && x.length == 3;

		E edge = getMesh().getEdge(face);
		V v = getMesh().getVertex(edge);
		x[0] = getMesh().getX(v);
		y[0] = getMesh().getY(v);
		z[0] = distances.getValue(v);

		v = getMesh().getVertex(getMesh().getNext(edge));
		x[1] = getMesh().getX(v);
		y[1] = getMesh().getY(v);
		z[1] = distances.getValue(v);

		v = getMesh().getVertex(getMesh().getPrev(edge));
		x[2] = getMesh().getX(v);
		y[2] = getMesh().getY(v);
		z[2] = distances.getValue(v);
	}

	default void getTriPoints(@NotNull final F face, double[] x, double[] y, double[] z, @NotNull final String name){
		assert x.length == y.length && y.length == z.length && x.length == 3;

		E edge = getMesh().getEdge(face);
		V v = getMesh().getVertex(edge);
		x[0] = getMesh().getX(v);
		y[0] = getMesh().getY(v);
		z[0] = getMesh().getDoubleData(v, name);

		v = getMesh().getVertex(getMesh().getNext(edge));
		x[1] = getMesh().getX(v);
		y[1] = getMesh().getY(v);
		z[1] = getMesh().getDoubleData(v, name);

		v = getMesh().getVertex(getMesh().getPrev(edge));
		x[2] = getMesh().getX(v);
		y[2] = getMesh().getY(v);
		z[2] = getMesh().getDoubleData(v, name);
	}

	default void getTriPoints(@NotNull final F face, double[] x, double[] y, double[] z, Function<V, Double> func){
		assert x.length == y.length && y.length == z.length && x.length == 3;

		E edge = getMesh().getEdge(face);
		V v = getMesh().getVertex(edge);
		x[0] = getMesh().getX(v);
		y[0] = getMesh().getY(v);
		z[0] = func.apply(v);

		v = getMesh().getVertex(getMesh().getNext(edge));
		x[1] = getMesh().getX(v);
		y[1] = getMesh().getY(v);
		z[1] = func.apply(v);

		v = getMesh().getVertex(getMesh().getPrev(edge));
		x[2] = getMesh().getX(v);
		y[2] = getMesh().getY(v);
		z[2] = func.apply(v);
	}

	/**
	 * Returns the quality of a face / triangle.
	 *
	 * @param face the face which has to be a valid triangle
	 * @return the quality of a face / triangle
	 */
	default double faceToQuality(final F face) {
		assert getMesh().getEdges(face).size() == 3;
		E edge = getMesh().getEdge(face);
		IPoint p1 = getMesh().getVertex(edge);
		IPoint p2 = getMesh().getVertex(getMesh().getNext(edge));
		IPoint p3 = getMesh().getVertex(getMesh().getPrev(edge));

		double quality = GeometryUtils.qualityInCircleOutCircle(p1, p2, p3);
		return GeometryUtils.qualityInCircleOutCircle(p1, p2, p3);
	}

	default double faceToLongestEdgeQuality(final F face) {
		E edge = getMesh().getEdge(face);
		IPoint p1 = getMesh().getVertex(edge);
		IPoint p2 = getMesh().getVertex(getMesh().getNext(edge));
		IPoint p3 = getMesh().getVertex(getMesh().getPrev(edge));
		return GeometryUtils.qualityLongestEdgeInCircle(p1, p2, p3);
	}

	default void getVirtualSupport(@NotNull final V v, @NotNull final E edge, @NotNull final List<Pair<V, V>> virtualSupport) {
		//assert isNonAcute(getMesh().getVertex(edge), getMesh().getVertex(getMesh().getNext(edge)), getMesh().getVertex(getMesh().getPrev(edge)));

		if(getMesh().isAtBoundary(edge)) {
			return;
		}

		E prev = getMesh().getPrev(edge);
		E twin = getMesh().getTwin(edge);

		V v1 = getMesh().getVertex(prev);
		V v2 = getMesh().getVertex(edge);
		V u = getMesh().getVertex(getMesh().getNext(twin));

		if(!isNonAcute(u, v, v1)) {
			virtualSupport.add(Pair.of(v1, u));
		} else {
			getVirtualSupport(v, getMesh().getNext(twin), virtualSupport);
		}

		if(!isNonAcute(v2, v, u)) {
			virtualSupport.add(Pair.of(v2, u));
		} else {
			getVirtualSupport(v, getMesh().getPrev(twin), virtualSupport);
		}

	}

	default boolean isNonAcute(V v1, V v2, V v3) {
		double angle1 = GeometryUtils.angle(v1, v2, v3);

		// non-acute triangle
		double rightAngle = Math.PI/2;
		return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
	}
}
