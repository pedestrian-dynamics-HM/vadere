package org.vadere.util.geometry.mesh.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 * @param <E>
 * @param <F>
 */
public interface ITriConnectivity<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> extends IPolyConnectivity<P, E, F> {

	default void splitFaceEvent(F original, F... faces) {}

	default void flipEdgeEvent(F f1, F f2) {}

	default void insertEvent(E vertex) {};

	boolean isIllegal(E edge);

	/**
	 * Splits the half-edge at point p, preserving a valid triangulation.
	 *
	 * @param p         the split point
	 * @param halfEdge  the half-edge which will be split
	 */
	default List<E> splitEdge(@NotNull P p, @NotNull E halfEdge, boolean legalize) {
		IMesh<P, E, F> mesh = getMesh();
		mesh.insertVertex(p);
		List<E> newEdges = new ArrayList<>(4);
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

		P v2 = mesh.getVertex(o0);
		F f0 = mesh.getFace(h0);
		F f3 = mesh.getFace(o0);

		// faces correct?
		mesh.createEdge(v2, mesh.getFace(o0));
		E e1 = mesh.createEdge(v2, mesh.getFace(o0));
		E t1 = mesh.createEdge(p, mesh.getFace(h0));
		mesh.setTwin(e1, t1);
		mesh.setVertex(o0, p);
		newEdges.add(t1);
		newEdges.add(h0);

		if(!mesh.isBoundary(h0)) {
			F f1 = mesh.createFace();

			E h1 = mesh.getNext(h0);
			E h2 = mesh.getNext(h1);

			P v1 = mesh.getVertex(h1);
			E e0 = mesh.createEdge(v1, f1);
			E t0 = mesh.createEdge(p, f0);

			mesh.setTwin(e0, t0);
			newEdges.add(t0);

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

			splitFaceEvent(f0, f0, f1);
		}
		else {
			mesh.setNext(mesh.getPrev(h0), t1);
			mesh.setNext(t1, h0);
		}

		if(!mesh.isBoundary(o0)) {
			E o1 = mesh.getNext(o0);
			E o2 = mesh.getNext(o1);

			P v3 = mesh.getVertex(o1);
			F f2 = mesh.createFace();

			// face
			E e2 = mesh.createEdge(v3, mesh.getFace(o0));
			E t2 = mesh.createEdge(p, f2);
			mesh.setTwin(e2, t2);
			newEdges.add(t2);

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

			splitFaceEvent(f3, f3, f2);
		}
		else {
			mesh.setNext(e1, mesh.getNext(o0));
			mesh.setNext(o0, e1);
		}

		if(legalize) {
			if(!mesh.isBoundary(h0)) {
				E h1 = mesh.getNext(h0);
				E h2 = mesh.getPrev(t1);
				legalize(h1);
				legalize(h2);
			}

			if(!mesh.isBoundary(o0)) {
				E o1 = mesh.getNext(e1);
				E o2 = mesh.getPrev(o0);
				legalize(o1);
				legalize(o2);
			}
		}

		return newEdges;
	}

	default List<E> splitEdge(@NotNull P p, @NotNull E halfEdge) {
		return splitEdge(p, halfEdge, true);
	}

	/**
	 * Flips an edge in the triangulation assuming the egdge which will be
	 * created is not jet there.
	 *
	 * @param edge the edge which will be flipped.
	 */
	default void flip(@NotNull final E edge) {
		IMesh<P, E, F> mesh = getMesh();

		// 1. gather all the references required
		E a0 = edge;
		E a1 = mesh.getNext(a0);
		E a2 = mesh.getNext(a1);

		E b0 = mesh.getTwin(edge);
		E b1 = mesh.getNext(b0);
		E b2 = mesh.getNext(b1);

		F fa = mesh.getFace(a0);
		F fb = mesh.getFace(b0);

		if(mesh.getEdge(fb).equals(b1)) {
			mesh.setEdge(fb, a1);
		}

		if(mesh.getEdge(fa).equals(a1)) {
			mesh.setEdge(fa, b1);
		}

		mesh.setVertex(a0, mesh.getVertex(a1));
		mesh.setVertex(b0, mesh.getVertex(b1));

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

	default boolean isTriangle(F face) {
		IMesh<P, E, F> mesh = getMesh();
		List<E> edges = mesh.getEdges(face);
		return edges.size() == 3;
	}

	default boolean insert(@NotNull F face, @NotNull P p) {
		throw new UnsupportedOperationException("not jet implemented.");
	}

	/**
	 * Splits the triangle xyz into three new triangles xyp, yzp and zxp.
	 *
	 * @param p       the point which splits the triangle
	 * @param face    the triangle face we split
	 *
	 * returns a half-edge which has p as its end vertex
	 */
	default List<F> splitTriangle(@NotNull F face, P p, boolean legalize) {
		assert isTriangle(face);
		getMesh().insertVertex(p);
		List<F> faceList = new ArrayList<>(3);
		IMesh<P, E, F> mesh = getMesh();
		List<E> edges = mesh.getEdges(face);

		F xyp = mesh.createFace();
		F yzp = mesh.createFace();
		F zxp = mesh.createFace();

		E zx = edges.get(0);
		E xy = edges.get(1);
		E yz = edges.get(2);

		P x = mesh.getVertex(zx);
		P y = mesh.getVertex(xy);
		P z = mesh.getVertex(yz);

		E yp = mesh.createEdge(p, xyp);
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

		// TODO: maybe we do not destroy the face, instead use it.
		mesh.destroyFace(face);

		faceList.add(xyp);
		faceList.add(yzp);
		faceList.add(zxp);

		splitFaceEvent(face, xyp, yzp, zxp);

		if(legalize) {
			legalize(zx);
			legalize(xy);
			legalize(yz);
		}

		return faceList;
	}

	default List<F> splitTriangle(@NotNull F face, P p) {
		return splitTriangle(face, p, true);
	}

	/**
	 * Legalizes an edge xy of a triangle xyz if it is illegal by flipping it.
	 *
	 * @param edge  an edge zx of a triangle xyz
	 */
	default void legalize(E edge) {
		if(isIllegal(edge)) {
			assert isFlipOk(edge);

			P p = getMesh().getVertex(getMesh().getNext(edge));

			F f1 = getMesh().getFace(edge);
			F f2 = getMesh().getTwinFace(edge);

			flip(edge);

			P vertex = getMesh().getVertex(edge);

			if(vertex.equals(p)) {
				E e1 = getMesh().getPrev(edge);
				E e2 = getMesh().getPrev(getMesh().getTwin(edge));
				legalize(e1);
				legalize(getMesh().getPrev(e2));
			}
			else {
				E e1 = getMesh().getNext(edge);
				E e2 = getMesh().getNext(getMesh().getTwin(edge));
				legalize(e1);
				legalize(getMesh().getNext(e2));
			}


		}
	}

	/**
	 * Tests if a flip for this half-edge is valid, i.e. the edge does not already exist.
	 *
	 * @param halfEdge the half-edge that might be flipped
	 * @return true if and only if the flip is valid
	 */
	default boolean isFlipOk(final E halfEdge) {
		if(getMesh().isBoundary(halfEdge)) {
			return false;
		}
		else {
			E xy = halfEdge;
			E yx = getMesh().getTwin(halfEdge);

			if(getMesh().getVertex(getMesh().getNext(xy)).equals(getMesh().getVertex(getMesh().getNext(yx)))) {
				return false;
			}

			P vertex = getMesh().getVertex(getMesh().getNext(yx));
			for(E neigbhour : getMesh().getIncidentEdgesIt(getMesh().getNext(xy))) {

				if(getMesh().getVertex(neigbhour).equals(vertex)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	default Optional<F> locate(final P point) {
		return this.locate(point.getX(), point.getY());
	}

	@Override
	default Optional<F> locate(final double x, final double y) {
		if(getMesh().getNumberOfFaces() > 0) {
			return locate(x, y, getMesh().getFace());
		}
		else {
			return Optional.empty();
		}
	}

	/*default Optional<P> locateVertex(double x, double y, F startFace) {
		Optional<F> optFace = locate(x, y, startFace);

		if(optFace.isPresent()) {
			F face = optFace.get();

			assert !getMesh().isBoundary(face);


		}

		return Optional.empty();
	}*/

	default Optional<F> locate(double x, double y, F startFace) {
		// there is no face.
		if(getDimension() <= 0 ){
			return Optional.empty();
		}

		if(getDimension() == 1) {
			return marchLocate1D(x, y, startFace);
		}
		else {
			VTriangle triangle = getMesh().toTriangle(startFace);
			VPoint incenter = triangle.getIncenter();
			return Optional.of(marchLocate2D(incenter.getX(), incenter.getY(), x, y, startFace));
		}
	}

	default Optional<F> marchLocate1D(double x, double y, F startFace) {
		if(contains(startFace, x, y)) {
			return Optional.of(startFace);
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Marching to the face which contains the point defined by (x2, y2). The march
	 * starts at (x1, y2) at inside the startFace.
	 *
	 * @param x1        the x-coordinate of the starting point
	 * @param y1        the y-coordinate of the starting point
	 * @param x2        the x-coordinate of the ending point
	 * @param y2        the y-coordinate of the ending point
	 * @param startFace the face where the march start containing (x1,y1).
	 * @return
	 */
	default F marchLocate2D(double x1, double y1, double x2, double y2, F startFace) {
		assert !getMesh().isBoundary(startFace);
		assert contains(startFace, x1, y1);

		E entryEdge = null;
		F face = startFace;
		boolean found = true;

		while(found && !contains(face, x2, y2)) {
			found = false;
			for(E halfEdge : getMesh().getEdgeIt(face)) {
				if(!halfEdge.equals(entryEdge)) {
					P sVertex = getMesh().getVertex(getMesh().getPrev(halfEdge));
					P eVertex = getMesh().getVertex(halfEdge);

					if(VLine.linesIntersect(sVertex.getX(), sVertex.getY(), eVertex.getX(), eVertex.getY(), x1, y1, x2, y2)) {
						entryEdge = getMesh().getTwin(halfEdge);
						face = getMesh().getTwinFace(halfEdge);
						found = true;
						break;
					}
				}
			}
		}

		assert found || getMesh().isBoundary(face);

		return face;
	}

	default boolean contains(F face, double x, double y) {


		if(getMesh().isMember(face, x, y)) {
			return true;
		}

		// in this case the face might be a polygon
		if(getMesh().isBoundary(face)) {
			return getMesh().toPolygon(face).contains(x, y);
		}
		else {
			return getMesh().toTriangle(face).contains(x, y);
		}
	}

	//Optional<F> marchLocate2DLFC(double x, double y, F startFace);

	default P getMaxDistanceVertex(F face, double x, double y) {
		List<P> vertices = getMesh().getVertices(face);

		assert vertices.size() == 3;

		P result = vertices.get(0);
		result = result.distance(x, y) < vertices.get(1).distance(x, y) ? vertices.get(1) : result;
		result = result.distance(x, y) < vertices.get(2).distance(x, y) ? vertices.get(2) : result;
		return result;
	}

	default P getMinDistanceVertex(F face, double x, double y) {
		List<P> vertices = getMesh().getVertices(face);

		assert vertices.size() == 3;

		P result = vertices.get(0);
		result = result.distance(x, y) > vertices.get(1).distance(x, y) ? vertices.get(1) : result;
		result = result.distance(x, y) > vertices.get(2).distance(x, y) ? vertices.get(2) : result;
		return result;
	}

	default int getDimension() {
		return getMesh().getNumberOfVertices() - 2;
	}

	/**
	 * Returns a Collection of faces which contain the point (x,y). In case of the point lying on the
	 * boundary of the face, the face contains the point. If the point lies on a vertex of the face,
	 * the face contains the point as well.
	 *
	 * @param x the x-coordinate of the point
	 * @param y the y-coordinate of the point
	 * @return a Collection of faces which contain the point.
	 */
	default Collection<F> getAdjacentFaces(final double x, final double y) {
		Optional<F> optFace = locate(x, y);
		if(optFace.isPresent()) {
			F face = optFace.get();

			/*
			 * the point (x,y) may
			 * 1. lies on a vertex of the face,
			 * 2. lies on a edge of the face,
			 * 3. lies on the interior of the face
			 */
			Optional<E> optMemberEdge = getMesh().getMemberEdge(face, x, y);
			if(optMemberEdge.isPresent()) {
				return getMesh().getAdjacentFaces(optMemberEdge.get());
			}
			else {
				List<F> faces = new ArrayList<>(2);
				faces.add(face);

				for(E halfEdge : getMesh().getEdgeIt(face)) {
					P v1 = getMesh().getVertex(halfEdge);
					P v2 = getMesh().getVertex(getMesh().getPrev(halfEdge));

					// TODO: think about the epsilon, absolute value seems to be a really bad idea!
					if(!getMesh().isBoundary(getMesh().getTwinFace(halfEdge)) && Math.abs(GeometryUtils.sign(x, y, v1.getX(), v1.getY(), v2.getX(), v2.getY())) == 0.0) {
						faces.add(getMesh().getTwinFace(halfEdge));
						break;
					}
				}

				return faces;
			}

		}
		else {
			return Collections.emptyList();
		}
	}
}
