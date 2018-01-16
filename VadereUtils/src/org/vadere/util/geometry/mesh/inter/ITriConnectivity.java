package org.vadere.util.geometry.mesh.inter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.mesh.iterators.Ring1Iterator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

//TODO: check unused methods!
/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 * @param <E>
 * @param <F>
 */
public interface ITriConnectivity<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends IPolyConnectivity<P, V, E, F> {

	Logger log = LogManager.getLogger(ITriConnectivity.class);
	Random random = new Random();

	/**
	 * Non mesh changing method.
	 *
	 * @param original
	 * @param f1
	 * @param f2
	 * @param f3
	 */
	default void splitTriangleEvent(F original, F f1, F f2, F f3) {}

	default void splitEdgeEvent(F original, F f1, F f2) {}

	default Iterable<E> getRing1It(V vertex) {
		return ()  -> new Ring1Iterator<>(getMesh(), getMesh().getEdge(vertex));
	}

	/**
	 * Inserts a point into the mesh which is contained in the boundary. The point will be
	 * connected by the start and end point of the edge. Note that the user has to make sure
	 * that a valid triangulation will obtained!
	 *
	 * @param point
	 * @param edge
	 * @return
	 */
	default F insertOutsidePoint(P point, E edge) {
		assert getMesh().isBoundary(edge);
		Optional<V> optVertex = getMesh()
				.streamVertices()
				.filter(v -> v.equals(getMesh().getPoint(v)))
				.findAny();

		V vertex = optVertex.orElse(getMesh().createVertex(point));
		F face = getMesh().createFace();
		F borderFace = getMesh().getFace(edge);

		E prev = getMesh().getPrev(edge);
		E next = getMesh().getNext(edge);

		E e1 = getMesh().createEdge(vertex);
		getMesh().setFace(e1, face);
		E e2 = getMesh().createEdge(getMesh().getVertex(prev));
		getMesh().setFace(e2, face);

		E b1 = getMesh().createEdge(vertex);
		getMesh().setFace(b1, borderFace);
		E b2 = getMesh().createEdge(getMesh().getVertex(edge));
		getMesh().setFace(b2, borderFace);

		getMesh().setNext(prev, b1);
		getMesh().setNext(b1, b2);
		getMesh().setNext(b2, next);

		getMesh().setNext(edge, e1);
		getMesh().setNext(e1, e2);
		getMesh().setNext(e2, edge);

		getMesh().setTwin(b1, e2);
		getMesh().setTwin(b2, e1);

		getMesh().setEdge(vertex, e1);
		getMesh().setEdge(borderFace, b1);
		getMesh().setEdge(face, e1);

		getMesh().setFace(edge, face);

		return face;
	}

	/**
	 * Non mesh changing method.
	 *
	 * @param f1
	 * @param f2
	 */
	default void flipEdgeEvent(F f1, F f2) {}

	/**
	 * Non mesh changing method.
	 *
	 * @param vertex
	 */
	default void insertEvent(E vertex) {};

	/**
	 * Non mesh changing method.
	 *
	 * @param edge
	 * @return
	 */
	boolean isIllegal(E edge, V p);

	default boolean isIllegal(E edge) {
		return isIllegal(edge, getMesh().getVertex(getMesh().getNext(edge)));
	}

	/**
	 * Splits the half-edge at point p, preserving a valid triangulation.
	 * Assumption: p is located on the edge!
	 * Mesh changing method.
	 *
	 * @param p         the split point
	 * @param halfEdge  the half-edge which will be split
	 * @return an newly created half-edge which has p as its end-point
	 */
	default E splitEdge(@NotNull P p, @NotNull E halfEdge, boolean legalize) {
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
			E t2 = mesh.createEdge(v, f2);
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

		return t1;
	}

	default E splitEdge(@NotNull P p, @NotNull E halfEdge) {
		return splitEdge(p, halfEdge, true);
	}


	/*default void flipLock(@NotNull final E edge) {

	}*/

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
	 * Flips an edge in the triangulation assuming the egdge which will be
	 * created is not jet there.
	 * mesh changing method.
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
	 * Tests if the face is counter-clockwise oriented.
	 * Assumption: The face is a triangle!
	 *
	 * @param triangleFace
	 * @return
	 */
	default boolean isCCW(F triangleFace) {
		E edge = getMesh().getEdge(triangleFace);
		P p1 = getMesh().getPoint(edge);
		P p2 = getMesh().getPoint(getMesh().getNext(edge));
		P p3 = getMesh().getPoint(getMesh().getPrev(edge));

		return GeometryUtils.isCCW(p1, p2, p3);
	}

	default boolean isTriangle(F face) {
		IMesh<P, V, E, F> mesh = getMesh();
		List<E> edges = mesh.getEdges(face);
		return edges.size() == 3;
	}

	E insert(@NotNull P p, @NotNull F face);

	/**
	 * Splits the triangle xyz into three new triangles xyp, yzp and zxp.
	 * Assumption: p is inside the face.
	 * Mesh changing method.
	 *
	 * @param point     the point which splits the triangle
	 * @param face      the triangle face we split
	 * @param legalize  true means that recursive filps will be done afterwards to legalizeRecursively illegal edges (e.g. delaunay requirement).
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
	 * Splits the triangle xyz into three new triangles xyp, yzp and zxp and legalizes all possibly illegal edges.
	 * Assumption: p is inside the face.
	 * Mesh changing method.
	 *
	 * @param p         the point which splits the triangle
	 * @param face      the triangle face we split
	 *
	 * returns a list of all newly created face.
	 */
	default E splitTriangle(@NotNull final F face, @NotNull final P p) {
		return splitTriangle(face, p, true);
	}

	/**
	 * Legalizes an edge xy of a triangle xyz if it is illegal by flipping it.
	 * Mesh changing method.
	 *
	 * @param edge  an edge zx of a triangle xyz
	 */
	default void legalizeRecursively(@NotNull final E edge, final V p, int depth) {
		if(isIllegal(edge, p)) {
			assert isFlipOk(edge);
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

	void legalizeNonRecursive(final E edge, final V p);

	default void legalize(final E edge, V p) {
		legalizeNonRecursive(edge, p);
	}

	/**
	 * Tests if a flip for this half-edge is valid, i.e. the edge does not already exist.
	 * Non mesh changing method.
	 *
	 * @param halfEdge the half-edge that might be flipped
	 * @return true if and only if the flip is valid
	 */
	default boolean isFlipOk(@NotNull final E halfEdge) {
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
	default Optional<F> locateFace(final P point) {
		return this.locateFace(point.getX(), point.getY());
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

		/*if(optFace.isPresent() && getMesh().isBoundary(optFace.get())) {
			return Optional.empty();
		}
		else {
			return optFace;
		}*/
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
	 * None mesh changing method.
	 *
	 * @param x
	 * @param y
	 * @param startFace
	 * @return
	 */
	default Optional<F> locateFace(double x, double y, F startFace) {
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

	default Optional<F> locateFace(P point, F startFace) {
		return locateFace(point.getX(), point.getY(), startFace);
	}

	/**
	 * None mesh changing method.
	 *
	 * @param x
	 * @param y
	 * @param startFace
	 * @return
	 */
	default Optional<F> marchLocate1D(double x, double y, F startFace) {
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
	 * Marching to the face which triangleContains the point defined by (x2, y2). Inside the startFace.
	 * This algorithm also works if there are convex polygon (holes) inside the triangulation.
	 * None mesh changing method.
	 *
	 * @param x1        the x-coordinate of the ending point
	 * @param y1        the y-coordinate of the ending point
	 * @param startFace the face where the march start containing (x1,y1).
	 * @return
	 */
	default F straightWalk2D(final double x1, final double y1, final F startFace) {
		return straightWalk2D(x1, y1, startFace, e -> !isRightOf(x1, y1, e));
	}

    default LinkedList<F> straightGatherWalk2D(final double x1, final double y1, final F startFace) {
        return straightGatherWalk2D(x1, y1, startFace, e -> !isRightOf(x1, y1, e));
    }

	/**
	 * Marching from a vertex which is the vertex of startVertex towards direction until the stopCondition is fulfilled.
	 *
	 * @param startVertex
	 * @param direction
	 * @param stopCondition
	 * @return
	 */
	default F straightWalk2D(final E startVertex, final F face, final VPoint direction, final Predicate<E> stopCondition) {

        E edge = getMesh().getPrev(startVertex);
		VPoint p1 = getMesh().toPoint(startVertex);


		assert intersects(p1, p1.add(direction), edge);
		assert getMesh().getEdges(face).contains(startVertex);
		// TODO: quick solution!
        VPoint q = p1.add(direction.scalarMultiply(Double.MAX_VALUE * 0.5));
		return straightWalk2D(p1, q, face, edge, e -> (stopCondition.test(e) || !isRightOf(q.x, q.y, e)));
	}

	default F straightWalk2D(final double x1, final double y1, final F startFace, final Predicate<E> stopCondition) {
		return straightGatherWalk2D(x1, y1, startFace, stopCondition).peekLast();
	}

    default LinkedList<F> straightGatherWalk2D(final double x1, final double y1, final F startFace, final Predicate<E> stopCondition) {
        assert !getMesh().isBoundary(startFace);

        // initialize
        F face = startFace;
        E edge = getMesh().getEdge(face);
        VPoint q = getMesh().toTriangle(startFace).getIncenter(); // walk from q to p
        VPoint p = new VPoint(x1, y1);

        return straightGatherWalk2D(q, p, face, edge, stopCondition);
    }

    /**
     * Walks along the line defined by q and p. The walking direction should be controlled by the stopCondition e.g.
     * e -> !isRightOf(x1, y1, e) stops the walk if (x1, y1) is on the left side of each edge which is the case if the
     * point is inside the reached face. The walk starts at the startFace and continues in the direction of line defined
     * by q and p using the any edge which does not fulfill the stopCondition.
     *
     * @param q
     * @param p
     * @param startFace
     * @param startEdge
     * @param stopCondition
     * @return
     */
	default F straightWalk2D(final VPoint q, final VPoint p, final F startFace, final E startEdge, final Predicate<E> stopCondition) {
        return straightGatherWalk2D(q, p, startFace, startEdge, stopCondition).peekLast();
	}

    // TODO: choose a better name
    default LinkedList<F> straightGatherWalk2D(final VPoint q, final VPoint p, final F startFace, final E startEdge, final Predicate<E> stopCondition) {
	    LinkedList<F> visitedFaces = new LinkedList<>();
	    visitedFaces.addLast(startFace);

        Optional<E> optEdge;
        F face = startFace;
        E edge = startEdge;

        do {
            optEdge = getMesh().streamEdges(edge).filter(e -> !stopCondition.test(e) && intersects(q, p, e)).findAny();

            if(optEdge.isPresent()) {
                edge = getMesh().getTwin(optEdge.get());
                face = getMesh().getTwinFace(optEdge.get());
                visitedFaces.addLast(face);

                // special case: hitting the boundary, this might be the outer boundary or a hole!
                if(getMesh().isBoundary(face)) {
                    VPoint p1 = getMesh().toPoint(getMesh().getVertex(edge));
                    optEdge = getMesh().streamEdges(edge).filter(e -> !stopCondition.test(e) && intersects(q, p, e)).findAny();

                    if(optEdge.isPresent()) {
                        VPoint p2 = getMesh().toPoint(getMesh().getVertex(optEdge.get()));

                        // Indicator that we reached the outer boundary: TODO: this test is not fully stable!
                        if(p.distance(p1) <= p.distance(p2)) {
                            break;
                        }
                        // else: we walk through a hole!
                    }
                    else {
                        break;
                    }
                }
            }
        } while (optEdge.isPresent());

        return visitedFaces;
    }

	default boolean isOuterBoundary(final F face) {
		if(getMesh().isBoundary(face)) {
			E boundaryEdge = getMesh().getEdge(face);
			VPoint p = getMesh().toPoint(getMesh().getVertex(getMesh().getNext(boundaryEdge)));
			return isRightOf(p.getX(), p.getY(), boundaryEdge);
		}
		else {
			return false;
		}
	}

	/**
	 * Marching to the face which triangleContains the point defined by (x2, y2). Inside the startFace.
	 * This algorithm does NOT works if there are convex polygon (holes) inside the triangulation.
	 * None mesh changing method.
	 *
	 * @param x1        the x-coordinate of the ending point
	 * @param y1        the y-coordinate of the ending point
	 * @param startFace the face where the march start containing (x1,y1).
	 * @return
	 */
	default F marchRandom2D(double x1, double y1, F startFace) {
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

	/**
	 * Returns the point which is closest to the p = (x1, y1) assuming it is in the
	 * circumcircle defined by the face.
	 *
	 * @param x1        the x-coordinate of the starting point
	 * @param y1        the y-coordinate of the starting point
	 * @param face      the face where the march start containing (x1,y1).
	 * @return
	 */
	default V locateNearestNeighbour(double x1, double y1, F face) {
		assert isInsideCircumscribedCycle(face, x1, y1);

		V nn = getNearestPoint(face, x1, y1);
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

	//TODO: rename
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
	}

	/**
	 * Tests if the point p = (x1, y1) is inside the circumscribedcycle defined by the triangle of the face.
	 * @param face  the face
	 * @param x1    x-coordinate of the point
	 * @param y1    y-coordinate of the point
	 * @return true if the point p = (x1, y1) is inside the circumscribedcycle defined by the triangle of the face, false otherwise.
	 */
	default boolean isInsideCircumscribedCycle(final F face, double x1, double y1) {
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);

		V v1 = getMesh().getVertex(e1);
		V v2 = getMesh().getVertex(e2);
		V v3 = getMesh().getVertex(e3);

		return GeometryUtils.isInsideCircle(v1, v2, v3, x1, y1);
	}

	/**
	 * Returns vertex of the triangulation of the face with the smallest distance to point.
	 * Assumption: The face has to be part of the mesh of the triangulation.
	 *
	 * @param face          the face of the trianuglation
	 * @param point         the point
	 * @return vertex of the triangulation of the face with the smallest distance to point
	 */
	default V getNearestPoint(final F face, final P point) {
		return getNearestPoint(face, point.getX(), point.getY());
	}

	default V getNearestPoint(final F face, final double x, final double y) {
		IMesh<P, V, E, F> mesh = getMesh();
		return mesh.streamEdges(face).map(edge -> mesh.getVertex(edge)).reduce((p1, p2) -> p1.distance(x,y) > p2.distance(x,y) ? p2 : p1).get();
	}


	/**
	 * None mesh changing method.
	 *
	 * @param face
	 * @param x
	 * @param y
	 * @return
	 */
	/*default boolean contains(F face, double x, double y) {
		if(isMember(face, x, y)) {
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
			return GeometryUtils.triangleContains(getMesh().getPoint(h1), getMesh().getPoint(h2), getMesh().getPoint(h3), new VPoint(x, y));
		}
	}*/

	//Optional<F> marchLocate2DLFC(double x, double y, F startFace);

	/**
	 * None mesh changing method.
	 *
	 * @param face
	 * @param x
	 * @param y
	 * @return
	 */
	default V getMaxDistanceVertex(F face, double x, double y) {
		List<V> vertices = getMesh().getVertices(face);

		//assert vertices.size() == 3;

		V result = vertices.get(0);
		result = result.distance(x, y) < vertices.get(1).distance(x, y) ? vertices.get(1) : result;
		result = result.distance(x, y) < vertices.get(2).distance(x, y) ? vertices.get(2) : result;
		return result;
	}

	/**
	 * None mesh changing method.
	 * @param face
	 * @param x
	 * @param y
	 * @return
	 */
	default V getMinDistanceVertex(F face, double x, double y) {
		List<V> vertices = getMesh().getVertices(face);

		//assert vertices.size() == 3;

		V result = vertices.get(0);
		result = result.distance(x, y) > vertices.get(1).distance(x, y) ? vertices.get(1) : result;
		result = result.distance(x, y) > vertices.get(2).distance(x, y) ? vertices.get(2) : result;
		return result;
	}

	default int getDimension() {
		return getMesh().getNumberOfVertices() - 2;
	}

	/**
	 * Returns the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
	 * Three cases are possible:
	 *  1) p is in the interior of the face
	 *  2) p lies on the edge which will be returned.
	 *  3) p is a vertex of the mesh
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
	 * Returns the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
	 * Two cases are possible:
	 *  1) p is in the interior of the face
	 *  2) p lies on the edge which will be returned.
	 *  3) p is a vertex of the mesh
	 *
	 * @param x         x-coordinate of the point p
	 * @param y         y-coordinate of the point p
	 * @param startFace the face the search will start from
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
	 * Returns the closest vertex of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
	 * Note that this might not be the closest point with respect to p in general. There might be a point closer which
	 * is however not part of the triangle which contains p.
	 *
	 * @param x x-coordinate of the point p
	 * @param y y-coordinate of the point p
	 * @return the closest vertex of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
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
	 * Returns the closest vertex of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
	 * Note that this might not be the closest point with respect to p in general. There might be a point closer which
	 * is however not part of the triangle which contains p. The search for the face containing p starts at the startFace.
	 *
	 * @param x         x-coordinate of the point p
	 * @param y         y-coordinate of the point p
	 * @param startFace the face the search will start from
	 * @return the closest vertex of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
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

	default boolean isMember(F face, double x, double y, double epsilon) {
		return getMemberEdge(face, x, y, epsilon).isPresent();
	}

	default Optional<E> getMemberEdge(F face, double x, double y,  double epsilon) {
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);

		if(getMesh().getPoint(e1).distance(x, y) < epsilon) {
			return Optional.of(e1);
		}

		if(getMesh().getPoint(e2).distance(x, y) < epsilon) {
			return Optional.of(e2);
		}

		if(getMesh().getPoint(e3).distance(x, y) < epsilon) {
			return Optional.of(e3);
		}

		return Optional.empty();
	}

	default boolean isMember(F face, double x, double y) {
		return getMemberEdge(face, x, y).isPresent();
	}

	default Optional<E> getMemberEdge(F face, double x, double y) {
		assert !getMesh().isBoundary(face);
		E e1 = getMesh().getEdge(face);
		P p1 = getMesh().getPoint(e1);

		if(p1.getX() == x && p1.getY() == y) {
			return Optional.of(e1);
		}

		E e2 = getMesh().getNext(e1);
		P p2 = getMesh().getPoint(e2);

		if(p2.getX() == x && p2.getY() == y) {
			return Optional.of(e2);
		}

		E e3 = getMesh().getNext(e2);
		P p3 = getMesh().getPoint(e3);

		if(p3.getX() == x && p3.getY() == y) {
			return Optional.of(e3);
		}

		return Optional.empty();
	}

}
