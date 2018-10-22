package org.vadere.geometry.mesh.inter;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.GeometryUtils;
import org.vadere.geometry.shapes.IPoint;
import org.vadere.geometry.shapes.VPoint;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

/**
 * <p>A tri-connectivity {@link ITriConnectivity} is the connectivity of a mesh of non-intersecting connected triangles including holes.
 * A hole can be an arbitrary simple polygon. So it is more concrete than a poly-connectivity {@link IPolyConnectivity}.
 * The mesh {@link IMesh} stores all the date of the base elements (points {@link P}, vertices {@link V}, half-edges {@link E}
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
 * @param <P> the type of the points (containers)
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 */
public interface ITriConnectivity<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends IPolyConnectivity<P, V, E, F> {

	/**
	 * A logger for debug and information reasons.
	 */
	Logger log = LogManager.getLogger(ITriConnectivity.class);

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
	 */
	default void splitTriangleEvent(@NotNull final F original, @NotNull final F f1, @NotNull final F f2, @NotNull final F f3) {}

	/**
	 * <p>This will be called whenever a triangle / face is split at a specific edge which
	 * will split it into tow faces. The method informs all listeners about that event.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param original  the original face
	 * @param f1        one of the split results
	 * @param f2        one of the split results
	 */
	default void splitEdgeEvent(@NotNull final F original, @NotNull final F f1, @NotNull final F f2) {}

	/**
	 * <p>This will replace the point of a vertex. If the point has other coordinates than
	 * the old point of the vertex this will reposition the vertex without any checks, i.e.
	 * the user has to know what he does and has to make sure that the mesh is valid and feasible
	 * afterwards and all listeners e.g. the point locators such as the Delaunay-Hierarchy
	 * {@link org.vadere.geometry.mesh.gen.DelaunayHierarchy} can handle this
	 * repositioning!</p>
	 *
	 * <p>Does not change the connectivity but may change the position of a vertex and therefore requires
	 * connectivity changes which has to be made manually!</p>
	 *
	 * @param vertex    the vertex
	 * @param point     the new point of the vertex
	 */
	default void replacePoint(@NotNull final V vertex, @NotNull final P point) {
		assert GeometryUtils.toPolygon(getMesh().getPoint(vertex)).contains(point);
		getMesh().setPoint(vertex, point);
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
	default F insertOutsidePoint(@NotNull final P point, @NotNull final E boundaryEdge, @NotNull final F boundary) {
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
	default Pair<E, E> splitEdge(@NotNull P p, @NotNull E halfEdge, boolean legalize) {
        IMesh<P, V, E, F> mesh = getMesh();
        V v = mesh.createVertex(p);
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

            splitEdgeEvent(f0, f0, f1);
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

            splitEdgeEvent(f3, f3, f2);
        }
        else {
            mesh.setNext(e1, mesh.getNext(o0));
            mesh.setNext(o0, e1);
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
	default Pair<E, E> splitEdge(@NotNull final P p, @NotNull final E halfEdge) {
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
		VPoint midPoint = getMesh().toLine(halfEdge).midPoint();
		P p = getMesh().createPoint(midPoint.getX(), midPoint.getY());
		return splitEdge(p, halfEdge, legalize);
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
		IMesh<P, V, E, F> mesh = getMesh();

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

		IMesh<P, V, E, F> mesh = getMesh();

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
		P p1 = getMesh().getPoint(edge);
		P p2 = getMesh().getPoint(getMesh().getNext(edge));
		P p3 = getMesh().getPoint(getMesh().getPrev(edge));

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
	E insert(@NotNull final P p, @NotNull final F face);

	/**
	 * <p>Splits the triangle xyz into three new triangles xyp, yzp and zxp. This requires amortized O(1) time.</p>
	 *
	 * <p>Assumption: p is inside the face.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param point     the point which splits the triangle
	 * @param face      the triangle face we split
	 * @param legalize  if true the triangulation will be legalized locally at the split to preserve a feasible triangulation
	 *
	 * @return an half-edge which has point as its end-point
	 */
	default E splitTriangle(@NotNull F face, @NotNull final P point, boolean legalize) {
		//assert isTriangle(face) && locateFace(point).get().equals(face);

		V p = getMesh().createVertex(point);
		getMesh().insertVertex(p);
		IMesh<P, V, E, F> mesh = getMesh();

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

		splitTriangleEvent(face, xyp, yzp, zxp);

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
	 * @param p         the point which splits the triangle
	 * @param face      the triangle face we split
	 *
	 * @return a list of all newly created face.
	 */
	default E splitTriangle(@NotNull final F face, @NotNull final P p) {
		return splitTriangle(face, p, true);
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
	default void remove2DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex) {
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
	}

	/**
	 * <p>Creates a new face by connecting two boundary vertices v1, v3 of a boundary path v1 to v2 to v3 such that
	 * v1 to v2 to v3 becomes a new face. This requires O(1) time.</p>
	 *
	 * Assumption:
	 * <ul>
	 *     <li>there is an counter clockwise angle smaller than 180 (PI) at v2 of the triangle (v1,v2,v3)</li>
	 *     <li>the boundaryEdge is a boundary edge</li>
	 * </ul>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param boundaryEdge an edge of the boundary (i.e. part of the border or a hole).
	 */
	default void createFaceAtBoundary(@NotNull final E boundaryEdge) {
		assert getMesh().isBoundary(boundaryEdge);

		F boundary = getMesh().getFace(boundaryEdge);
		E next = getMesh().getNext(boundaryEdge);
		E prev = getMesh().getPrev(boundaryEdge);

		// can we form a triangle
		assert GeometryUtils.isCCW(getMesh().toPoint(prev), getMesh().toPoint(boundaryEdge), getMesh().toPoint(next))
				&& GeometryUtils.angle(getMesh().toPoint(prev), getMesh().toPoint(boundaryEdge), getMesh().toPoint(next)) < Math.PI;

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

		IMesh<P, V, E, F> mesh = getMesh();
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

	@Override
	default Optional<F> locateFace(final double x, final double y) {
		Optional<F> optFace;
		if(getMesh().getNumberOfFaces() > 1) {
			optFace = locateFace(x, y, getMesh().getFace());
		}
		else if(getMesh().getNumberOfFaces() == 1) {
			optFace = Optional.of(getMesh().getFace());
		}
		else {
			optFace = Optional.empty();
		}

		return optFace;
	}

	/*default Optional<P> locateVertex(double x, double y, F startFace) {
		Optional<F> optFace = locateFace(x, y, startFace);

		if(optFace.isPresent()) {
			F face = optFace.get();

			assert !getMesh().isBoundary(face);


		}

		return Optional.empty();
	}*/

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
	default Optional<F> locateFace(final double x, final double y, @NotNull final F startFace) {
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
	default Optional<F> locateFace(@NotNull final P point, F startFace) {
		return locateFace(point.getX(), point.getY(), startFace);
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
	default E walkThroughHole(@NotNull VPoint q, @NotNull VPoint p, @NotNull E enteringEdge) {
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
	 * <p>Marching to the face which contains the point defined by (x2, y2) starting inside the startFace.
	 * This algorithm also works if there are convex polygon (holes) inside the triangulation.</p>
	 *
	 * <p>Assumption: (x, y) is contained in some face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x1        the x-coordinate of the ending point
	 * @param y1        the y-coordinate of the ending point
	 * @param startFace the face where the march start containing (x1,y1).
	 * @return returns the face containing (x, y)
	 */
	default F straightWalk2D(final double x1, final double y1, @NotNull final F startFace) {
		return straightWalk2D(x1, y1, startFace, e -> !isRightOf(x1, y1, e));
	}

	/**
	 * <p>Marching to the face which contains the point defined by (x2, y2) starting inside the startFace.
	 * Furthermore this method will gather all visited faces and requires O(n) time. However, if the face is close
	 * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
	 * inside the triangulation.</p>
	 *
	 * <p>Assumption: (x, y) is contained in some face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param x1        the x-coordinate of the point at which the march will start
	 * @param y1        the y-coordinate of the point at which the march will start
	 * @param startFace the face where the march start containing (x1,y1).
	 * @return returns all visited faces in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
	 */
    default LinkedList<F> straightGatherWalk2D(final double x1, final double y1, @NotNull final F startFace) {
        return straightGatherWalk2D(x1, y1, startFace, e -> !isRightOf(x1, y1, e));
    }

	/**
	 * Marching from a vertex which is the vertex (<tt>startVertex</tt>) of face (<tt>face</tt>) in the direction (<tt>direction</tt>)
	 * until the stop-condition (<tt>stopCondition</tt>) is fulfilled. This requires O(n) worst case time, where n is the number of faces of the mesh.
	 *
	 * <p>Assumption: The stopCondition will be fulfilled at some point.</p>
	 *
	 * @param startVertex       the vertex at which the march starts
	 * @param face              the face at which the march / search starts
	 * @param direction         the direction in which the march will go
	 * @param stopCondition     the stop condition at which the march will stop
	 * @return a face which is reached by starting a march from vertex and marching towards direction until stopCondition
	 */
	default F straightWalk2D(@NotNull final E startVertex, @NotNull final F face, @NotNull final VPoint direction, @NotNull final Predicate<E> stopCondition) {

        E edge = getMesh().getPrev(startVertex);
		VPoint p1 = getMesh().toPoint(startVertex);


		assert intersects(p1, p1.add(direction), edge);
		assert getMesh().getEdges(face).contains(startVertex);
		// TODO: quick solution!
        VPoint q = p1.add(direction.scalarMultiply(Double.MAX_VALUE * 0.5));
		return straightWalk2D(p1, q, face, e -> (stopCondition.test(e) || !isRightOf(q.x, q.y, e)));
	}

	default F straightWalk2D(final double x1, final double y1, @NotNull final F startFace, @NotNull final Predicate<E> stopCondition) {
		return straightGatherWalk2D(x1, y1, startFace, stopCondition).peekLast();
	}

	/**
	 * <p>Marches / walks along the line defined by q and p from q to p starting inside the startFace.
	 * Furthermore this method will gather all visited faces and requires O(n) time. However, if the face is close
	 * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
	 * inside the triangulation. A stop condition like (e to !isRightOf(x1, y1, e)) stops the walk if (x1, y1),
	 * will stop the walk if the point p = (x1, y1) is contained in the face.</p>
	 *
	 * Assumption:
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
    default LinkedList<F> straightGatherWalk2D(final double x1, final double y1, @NotNull final F startFace, @NotNull final Predicate<E> stopCondition) {
        assert !getMesh().isBorder(startFace);

        // initialize
        F face = startFace;
        VPoint q = getMesh().toPolygon(startFace).getCentroid(); // walk from q to p
        VPoint p = new VPoint(x1, y1);

        return straightGatherWalk2D(q, p, face, stopCondition);
    }

	/**
	 * <p>Connects each (current) two consecutive border edge if they form an acute angle
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

	/**
	 * <p>Connects each (current) two consecutive border edge if they form an acute angle
	 * which smoothes the border of the mesh overall. This requires O(n) time, where n
	 * is the number of border edges.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 */
	default void smoothHoles() {
		for(F hole : getMesh().getHoles()) {
			for(E edge : getMesh().getEdges(hole)) {
				if(getMesh().getFace(edge).equals(hole)) {

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
	}

	default void smoothBoundary() {
		smoothBorder();
		smoothHoles();
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
        return straightGatherWalk2D(q, p, startFace, stopCondition).peekLast();
	}

	// TODO change centroid to midpoint of a triangle!
	/**
	 * Walks one step i.e. from a face to immediate / neighbouring next face along the line defined by q and p
	 * from q to p. Furthermore this method will gather the visited face by placing it into the list of visited faces.
	 * There are different cases with special cases which make the code complicated:
	 * <ol>
	 *     <li>general case:    the line (q, p) intersects two half-edges.
	 *                          In this case the algorithm walks across the correct line by the definition of the direction</li>
	 *     <li>special case 1:  there is one point of the face which lies on the line (q, p) but the stop condition is not fulfilled for the corresponding half-edge.
	 *                          In this case the algorithm walks just across that corresponding half-edge</li>
	 *     <li>special case 2:  there is two point of the face which lies on the line (q, p) but p is contained or very close to the face.
	 *                          In this case the walk can return the face as (end-)walk-result.</li>
	 *     <li>special case 3:  there is two point of the face which lies on the line (q, p) and p is not contained in the face.
	 *                          This is a bad / expensive case since the algorithm will first move to the vertex v of the face which is closest to p.
	 *                          Then it will go into a neighbouring face of v which has the centroid closest to p and which is not contained in the
	 *                          list of visited faces. This can require O(n) where n is the number of faces but should never happen in a "normal" triangulation.</li>
	 * </ol>
	 *
	 * <p>Assumption: q is contained in the start face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param q             start point of the march / walk
	 * @param p             end point of the march / walk
	 * @param face          start face of the walk
	 * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
	 * @param visitedFaces  a list which will be filled with the visited faces in order in which they are visited (first visited = first in)
	 * @return all visited faces in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
	 */
	default Optional<F> straightWalkNext(
			@NotNull final F face,
			@NotNull final VPoint q,
			@NotNull final VPoint p,
			@NotNull final Predicate<E> stopCondition,
			@NotNull final LinkedList<F> visitedFaces) {
		E e1 = null;
		E e2 = null;

		/**
		 * Get the half-edges e which intersects (q, p).
		 */
		for(E e : getMesh().getEdgeIt(face)) {
			boolean intersect = intersects(q, p, e);

			if(intersect && e1 == null) {
				e1 = e;
			}
			else if(intersect) {
				e2 = e;
			}
		}

		/**
		 * General case (1): The line defined by (q,p) intersects 2 edges of the convex polygon.
		 */
		if(e2 != null) {
			//log.debug("straight walk: general case");
			if(!stopCondition.test(e1)) {
				return Optional.of(getMesh().getTwinFace(e1));
			}
			else if(!stopCondition.test(e2)) {
				return Optional.of(getMesh().getTwinFace(e2));
			}
			else {
				return Optional.empty();
			}
		}
		/**
		 * Special case (2): There is one or two points of the polygon which are collinear with the line defined by (q,p).
		 */
		else {
			// the unimportant point is collinear => n.p.
			/**
			 * Good case (2.1): There is only one collinear point and the exit edge of the polygon exist.
			 */
			if(e1 != null && !stopCondition.test(e1)) {
				//log.debug("straight walk: only one intersection line");
				return Optional.of(getMesh().getTwinFace(e1));
			}
			// q, the exit point and p are collinear :( or the point lies inside the face :)
			else {
				/**
				 * Good case (2.2) There are two collinear points but the face contains p => p lies on an edge of the face.
				 */
				if(contains(p.getX(), p.getY(), face) || getMesh().isCloseTo(face, p.getX(), p.getY())) {
					//log.debug("no intersection line and not contained.");
					return Optional.empty();
				}
				/**
				 * Bad case (2.3): This which should not happen in general: q, the exit point v and p are collinear, therefore there is no exit intersection line!
				 * We continue the search with the face which centroid is closest to p! v has to be the closest p as well.
				 */
				else {
//					log.debug("straight walk: no exit edge found due to collinear exit point.");

					/**
					 * Get the vertex v closest to p.
					 */
					V v = getMesh().streamVertices(face).min(Comparator.comparingDouble(p::distance)).get();

					/*SimpleTriCanvas canvas = SimpleTriCanvas.simpleCanvas(getMesh());
					getMesh().streamFaces(v).forEach(f -> canvas.getColorFunctions().overwriteFillColor(f, Color.MAGENTA));
					if(DebugGui.isDebugOn()) {
						DebugGui.showAndWait(canvas);
					}*/

					/**
					 * Get the face with the centroid closest to p and which was not visited already.
					 */
					Optional<F> closestFace = getMesh().streamFaces(v)
							.filter(f -> !getMesh().isBorder(f))
							.filter(f -> !visitedFaces.contains(f))
							.min(Comparator.comparingDouble(f -> p.distance(getMesh().toPolygon(f).getCentroid())));
					return closestFace;
				}
			}
		}
	}

	/**
	 * <p>Marches / walks along the line defined by q and p from q to p starting inside the startFace.
	 * Furthermore this method will gather all visited faces and requires O(n) time. However, if the face is close
	 * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
	 * inside the triangulation. A stop condition like (e to !isRightOf(x1, y1, e)) stops the walk if (x1, y1),
	 * will stop the walk if the point p = (x1, y1) is contained in the face.
	 * The method goes from one face to the next by calling {@link ITriConnectivity#straightWalkNext(IFace, VPoint, VPoint, Predicate, LinkedList)}
	 * but adds the resulting face to the list of visited faces and adds some logging to debug the walks / marches.</p>
	 *
	 * <p>Assumption: q is contained in the start face.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param q             start point of the march / walk
	 * @param p             end point of the march / walk
	 * @param startFace     start face of the walk
	 * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
	 * @return all visited faces in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
	 */
    default LinkedList<F> straightGatherWalk2D(final VPoint q, final VPoint p, final F startFace, final Predicate<E> stopCondition) {
		LinkedList<F> visitedFaces = new LinkedList<>();
	    visitedFaces.addLast(startFace);

	    assert contains(q.getX(), q.getY(), startFace);

        Optional<F> optFace;
        F face = startFace;

        do {

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
	        optFace = straightWalkNext(face, q, p, stopCondition, visitedFaces);

	        if(!optFace.isPresent()) {
	        	//log.info("expensive fix");
		        //optFace = straightWalkNext(face, q, p, stopCondition);
	        }
	        else {
	        	//log.info("fast");
	        }

            if(optFace.isPresent()) {
                face = optFace.get();
                visitedFaces.addLast(face);

                // special case (1): hitting the border i.e. outer boundary
	            // special case (2): hitting a hole
                if(getMesh().isBorder(face)) {
                   //log.debug("walked towards the border!");
                   // return the border
	               // log.debug(getMesh().toPath(face));
                   break;
                }
                else if(getMesh().isHole(face)) {
	                //log.debug("walked towards a hole!");
	                // just go on with the normal straight walk which works for CONVEX polygons!
                }
            }
        } while (optFace.isPresent());


	    //log.debug("start walk: from " + q + " to " + p + " by walking through:");
	    //visitedFaces.forEach(f -> log.debug(getMesh().toPath(f)));

	    /*if(!(getMesh().isBorder(visitedFaces.peekLast()) || contains(p.getX(), p.getY(), visitedFaces.peekLast()))) {
		    boolean test = contains(p.getX(), p.getY(), visitedFaces.peekLast());
	    }*/
	    assert getMesh().isBorder(visitedFaces.peekLast()) || contains(p.getX(), p.getY(), visitedFaces.peekLast());
        return visitedFaces;
    }


    /*default LinkedList<F> straightGatherWalk2D(final VPoint q, final VPoint p, final F startFace, final E startEdge, final Predicate<E> stopCondition) {
		log.info("start walk: from " + q + " to " + p);
		LinkedList<F> visitedFaces = new LinkedList<>();
		visitedFaces.addLast(startFace);

		Optional<E> optEdge;
		F face = startFace;

		do {
			log.info(getMesh().toPath(face));
			log.info("intersections: " + getMesh().streamEdges(face).filter(e -> intersects(q, p, e)).count());
			log.info("intersections & on the right: " + getMesh().streamEdges(face).filter(e -> !stopCondition.test(e) && intersects(q, p, e)).count());
			optEdge = getMesh().streamEdges(face).filter(e -> !stopCondition.test(e) && intersects(q, p, e)).findAny();

			if(optEdge.isPresent()) {
				face = getMesh().getTwinFace(optEdge.get());
				visitedFaces.addLast(face);

				// special case (1): hitting the border i.e. outer boundary
				// special case (2): hitting a hole
				if(getMesh().isBorder(face)) {
					log.info("walked towards the border!");
					// return the border
					log.info(getMesh().toPath(face));
					break;
				}
				else if(getMesh().isHole(face)) {
					log.info("walked towards a hole!");
					// just go on with the normal straight walk which works for CONVEX polygons!
				}
			}
		} while (optEdge.isPresent());

		assert getMesh().isBorder(visitedFaces.peekLast()) || contains(p.getX(), p.getY(), visitedFaces.peekLast());
		log.info("end walk");
		return visitedFaces;
	}*/

	/*default LinkedList<F> straightGatherWalk2D(final VPoint q, final VPoint p, final F startFace, final E startEdge, final Predicate<E> stopCondition) {
		log.info("start walk: from " + q + " to " + p);
		LinkedList<F> visitedFaces = new LinkedList<>();
		visitedFaces.addLast(startFace);

		Optional<E> optEdge;
		F face = startFace;
		E edge = startEdge;

		do {
			log.info(getMesh().toPath(face));
			optEdge = getMesh().streamEdges(edge).filter(e -> !stopCondition.test(e) && intersects(q, p, e)).findAny();

			if(optEdge.isPresent()) {
				edge = getMesh().getTwin(optEdge.get());
				face = getMesh().getTwinFace(optEdge.get());
				visitedFaces.addLast(face);

				// special case (1): hitting the border i.e. outer boundary
				// special case (2): hitting a hole
				if(getMesh().isBorder(face)) {
					log.info("walked towards the border!");
					// return the border
					log.info(getMesh().toPath(face));
					break;
				}
				else if(getMesh().isHole(face)) {
					log.info("walked towards a hole!");
					// just go on with the normal straight walk which works for CONVEX polygons!
				}
			}
		} while (optEdge.isPresent());

		assert getMesh().isBorder(visitedFaces.peekLast()) || contains(p.getX(), p.getY(), visitedFaces.peekLast());
		log.info("end walk");
		return visitedFaces;
	}*/

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
		Optional<F> optFace = locateFace(x, y);

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
		Optional<F> optFace = locateFace(x, y, startFace);

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
		Optional<F> optFace = locateFace(x, y);

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
		Optional<F> optFace = locateFace(x, y, startFace);

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
			P p1 = getMesh().getPoint(getMesh().getPrev(edge));
			P p2 = getMesh().getPoint(edge);
			P p3 = getMesh().getPoint(getMesh().getNext(edge));
			boolean valid = GeometryUtils.isLeftOf(p1, p2, p3);
			if(!valid) {
				log.info(p1 + ", " + p2 + ", " + p3);
			}
			return GeometryUtils.isLeftOf(p1, p2, p3);
		};

		return getMesh().streamFaces().filter(f -> !getMesh().isDestroyed(f)).filter(f -> !getMesh().isBoundary(f)).allMatch(orientationPredicate);
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
			P p1 = getMesh().getPoint(getMesh().getPrev(edge));
			P p2 = getMesh().getPoint(edge);
			P p3 = getMesh().getPoint(getMesh().getNext(edge));
			return GeometryUtils.isLeftOf(p1, p2, p3);
		};

		return !getMesh().isBoundary(face) && orientationPredicate.test(face);
	}
}
