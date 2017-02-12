package org.vadere.util.triangulation;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.data.DAG;
import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.data.HalfEdge;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.data.Triangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.*;

public class DelaunayTriangulation<P extends IPoint> implements Triangulation<P> {

	private Face<P> face;
	private final Set<P> points;
	private final PointConstructor<P> pointConstructor;
	private P p0;
	private P p1;
	private P p2;
	private HalfEdge<P> he0;
	private HalfEdge<P> he1;
	private HalfEdge<P> he2;
	private boolean finalized;

	private DAG<DAGElement<P>> dag;
	private final HashMap<Face<P>, DAG<DAGElement<P>>> map;
	private int count;
	private double eps = 0.0000001;
	private Face<P> superTriangle;
	private Face<P> borderFace;
	private static Logger log = LogManager.getLogger(DelaunayTriangulation.class);

	public DelaunayTriangulation(
			final Set<P> points,
			final PointConstructor<P> pointConstructor) {
		this.points = points;
		this.pointConstructor = pointConstructor;
		this.map = new HashMap<>();
		this.count = 0;
		P p_max = points.parallelStream().reduce(pointConstructor.create(Double.MIN_VALUE, Double.MIN_VALUE), (a, b) -> pointConstructor.create(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY())));
		P p_min = points.parallelStream().reduce(pointConstructor.create(Double.MIN_VALUE, Double.MIN_VALUE), (a, b) -> pointConstructor.create(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY())));
		init(p_max, p_min);
	}

	public DelaunayTriangulation(
			final double minX,
			final double minY,
			final double width,
			final double height,
			final PointConstructor<P> pointConstructor) {
		this.points = new HashSet<P>();
		this.pointConstructor = pointConstructor;
		this.map = new HashMap<>();
		this.count = 0;
		P p_max = pointConstructor.create(minX + width, minY + height);
		P p_min = pointConstructor.create(minX, minY);

		init(p_max, p_min);

	}

	private void init(final P p_max, final P p_min) {
		VRectangle bound = new VRectangle(p_min.getX(), p_min.getY(), p_max.getX()-p_min.getX(), p_max.getY()- p_min.getY());

		double gap = 1.0;
		double max = Math.max(bound.getWidth(), bound.getHeight())*2;
		p0 = pointConstructor.create(bound.getX() - max - gap, bound.getY() - gap);
		p1 = pointConstructor.create(bound.getX() + 2 * max + gap, bound.getY() - gap);
		p2 = pointConstructor.create(bound.getX() + (max+2*gap)/2, bound.getY() + 2 * max + gap);

		superTriangle = Face.of(p0, p1, p2);
		borderFace = superTriangle.getEdge().getTwin().getFace();

		List<HalfEdge<P>> borderEdges = borderFace.getEdges();
		he0 = borderEdges.get(0);
		he1 = borderEdges.get(1);
		he2 = borderEdges.get(2);


		this.dag = new DAG<>(new DAGElement<>(superTriangle, Triple.of(p0, p1, p2)));
		this.map.put(superTriangle, dag);
		this.count = 0;
		this.face = null;
		this.finalized = false;
	}

	@Override
	public void insert(P point) {
		Collection<DAG<DAGElement<P>>> leafs = locatePoint(point, true);
		assert leafs.size() == 2 ||leafs.size() == 1 ||leafs.size() == 0;
		count++;

		// point is inside a triangle
		if(leafs.size() == 1) {
			log.info("splitTriangle");
			splitTriangleDB(point, leafs.stream().findAny().get());
		} // point lies on an edge of 2 triangles
		else if(leafs.size() == 2) {
			Iterator<DAG<DAGElement<P>>> it = leafs.iterator();
			log.info("splitEdge");
			splitEdgeDB(point, findTwins(it.next().getElement().getFace(),  it.next().getElement().getFace()));
		}
		else if(leafs.size() == 0) {
			// problem due numerical calculation.
			log.warn("numerical error!");
		}
		else {
			log.warn("ignore insertion point, since this point already exists!");
		}
	}

	/**
	 * Removes the super triangle from the mesh data structure.
	 */
	public void finalize() {
		// we have to use other halfedges than he1 and he2 since they might be deleted
		// if we deleteBoundaryFace he0!
		List<Face<P>> faces1 = IteratorUtils.toList(he0.incidentFaceIterator());
		List<Face<P>> faces2 = IteratorUtils.toList(he1.incidentFaceIterator());
		List<Face<P>> faces3 = IteratorUtils.toList(he2.incidentFaceIterator());

		faces1.forEach(f -> deleteBoundaryFace(f));

		faces2.removeIf(f -> f.isDestroyed());
		faces2.forEach(f -> deleteBoundaryFace(f));

		faces3.removeIf(f -> f.isDestroyed());
		faces3.forEach(f -> deleteBoundaryFace(f));

		finalized = true;
	}

	public boolean isDeletionOk(final Face<P> face) {
		if(face.isDestroyed()) {
			return false;
		}

		for(HalfEdge<P> halfEdge : face) {
			if(halfEdge.getTwin().isBoundary()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Deletes a face assuming that the face contains at least one boundary edge, otherwise the
	 * deletion will not result in an feasibly triangulation.
	 *
	 * @param face the face that will be deleted, which as to be adjacent to the boundary.
	 */
	public void deleteBoundaryFace(final Face<P> face) {
		assert isDeletionOk(face);

		// 3 cases: 1. triangle consist of 1, 2 or 3 boundary edges
		List<HalfEdge<P>> boundaryEdges = new ArrayList<>(3);
		List<HalfEdge<P>> nonBoundaryEdges = new ArrayList<>(3);

		for(HalfEdge<P> halfEdge : face) {
			if(halfEdge.getTwin().isBoundary()) {
				boundaryEdges.add(halfEdge);
			}
			else {
				nonBoundaryEdges.add(halfEdge);
			}
		}

		if(boundaryEdges.size() == 3) {
			// release memory
			face.getEdges().forEach(halfEdge -> halfEdge.destroy());
		}
		else if(boundaryEdges.size() == 2) {
			HalfEdge<P> toB = boundaryEdges.get(0).getNext().getTwin().isBoundary() ? boundaryEdges.get(0) : boundaryEdges.get(1);
			HalfEdge<P> toF = boundaryEdges.get(0).getNext().getTwin().isBoundary() ? boundaryEdges.get(1) : boundaryEdges.get(0);

			HalfEdge<P> nB = nonBoundaryEdges.get(0);
			nB.setFace(toF.getTwin().getFace());
			nB.setNext(toB.getTwin().getNext());
			nB.setPrevious(toF.getTwin().getPrevious());

			toF.getTwin().getFace().setEdge(nB);

			// release memory
			toF.destroy();
			toB.destroy();
		}
		else {
			HalfEdge<P> boundaryHe = boundaryEdges.get(0);
			HalfEdge<P> prec = boundaryHe.getTwin().getPrevious();
			HalfEdge<P> succ = boundaryHe.getTwin().getNext();

			boundaryHe.getNext().setPrevious(prec);
			boundaryHe.getNext().setFace(boundaryHe.getTwin().getFace());
			boundaryHe.getPrevious().setNext(succ);
			boundaryHe.getPrevious().setFace(boundaryHe.getTwin().getFace());
			prec.getFace().setEdge(prec);

			// release memory
			boundaryHe.destroy();
		}

		face.destroy();
		map.remove(face);
	}

	@Override
	public void compute() {
		// 2. insert points
		for(P p : points) {
			insert(p);
			count++;
		}
	}

	@Override
	public Face<P> locate(final IPoint point) {
		Optional<DAG<DAGElement<P>>>  optDag = locatePoint(point, false).stream().findAny();
		if(optDag.isPresent()) {
			return optDag.get().getElement().getFace();
		}
		else {
			return null;
		}
	}

	@Override
	public Face<P> locate(final double x, double y) {
		return locate(new VPoint(x, y));
	}

	@Override
	public Set<Face<P>> getFaces() {
		return streamFaces().collect(Collectors.toSet());
	}

	@Override
	public Stream<Face<P>> streamFaces() {
		return stream();
		//return map.values().stream()
				/*.filter(dagElement -> {
					Triple<P, P, P> triple = dagElement.getElement().getVertices();
					Set<P> pointset = new HashSet<>();
					pointset.add(triple.getLeft());
					pointset.add(triple.getMiddle());
					pointset.add(triple.getRight());
					return !pointset.contains(p0) && !pointset.contains(p1) && !pointset.contains(p2);
				})*/
				//.map(dagElementDAG -> dagElementDAG.getElement().getFace());
	}

	/**
	 * Returns a half-edge such that it is part of face1 and the twin of this half-edge
	 * is part of face2.
	 *
	 * @param face1 the first face
	 * @param face2 the second face that might be a neighbour of face1
	 * @return  the half-edge of face1 such that its twin is part of face2
	 */
	private HalfEdge<P> findTwins(final Face<P> face1, final Face<P> face2) {
		for(HalfEdge<P> halfEdge1 : face1) {
			for(HalfEdge<P> halfEdge2 : face2) {
				if(halfEdge1.getTwin() == null) {
					System.out.print("ddd");
				}

				if(halfEdge1.getTwin().equals(halfEdge2)) {
					return halfEdge1;
				}
			}
		}
		return null;
	}

	@Override
	public void remove(P point) {
		throw new UnsupportedOperationException("not jet implemented.");
	}

	public Collection<VTriangle> getTriangles() {
		return stream().map(face -> faceToTriangle(face)).collect(Collectors.toSet());
		//return getFaces().stream().map(face -> faceToTriangle(face)).collect(Collectors.toSet());
	}

	/*public Collection<Triple<P, P, P>> getTrianglePoints() {
		return triangles.stream().map(dagElement -> dagElement.getElement().getVertices()).collect(Collectors.toList());
	}*/

	public Set<VLine> getEdges() {
		return getTriangles().stream().flatMap(triangle -> triangle.getLineStream()).collect(Collectors.toSet());
	}

	private VTriangle faceToTriangle(final Face<P> face) {
		List<P> points = face.getPoints();
		P p1 = points.get(0);
		P p2 = points.get(1);
		P p3 = points.get(2);
		return new VTriangle(new VPoint(p1.getX(), p1.getY()), new VPoint(p2.getX(), p2.getY()), new VPoint(p3.getX(), p3.getY()));
	}

	private Collection<DAG<DAGElement<P>>> locatePoint(final IPoint point, final boolean insertion) {

		Set<DAG<DAGElement<P>>> leafs = new HashSet<>();
		LinkedList<DAG<DAGElement<P>>> nodesToVisit = new LinkedList<>();
		nodesToVisit.add(dag);

		while(!nodesToVisit.isEmpty()) {
			DAG<DAGElement<P>> currentNode = nodesToVisit.removeLast();
			if(currentNode.getElement().getTriangle().isPartOf(point, eps)) {
				if(currentNode.isLeaf()) {
					leafs.add(currentNode);

					// if we are not interested in insertion we just want to find one triangle.
					if(!insertion) {
						return leafs;
					}
				}
				else {
					nodesToVisit.addAll(currentNode.getChildren());
				}
			}
		}

		return leafs;
	}


	/**
	 * Splits the half-edge at point p, preserving a valid triangulation.
	 *
	 * @param p         the split point
	 * @param halfEdge  the half-edge which will be split
	 */
	public void splitEdge(@NotNull P p, @NotNull HalfEdge<P> halfEdge) {

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

		HalfEdge<P> h0 = halfEdge;
		HalfEdge<P> o0 = h0.getTwin();

		P v2 = o0.getEnd();
		Face<P> f0 = h0.getFace();
		Face<P> f3 = o0.getFace();

		// faces correct?
		HalfEdge<P> e1 = new HalfEdge<>(v2, o0.getFace());
		HalfEdge<P> t1 = new HalfEdge<>(p, h0.getFace());
		e1.setTwin(t1);
		o0.setEnd(p);

		if(!h0.isBoundary()) {
			Face<P> f1 = new Face<>();

			HalfEdge<P> h1 = h0.getNext();
			HalfEdge<P> h2 = h1.getNext();

			P v1 = h1.getEnd();
			HalfEdge<P> e0 = new HalfEdge<>(v1, f1);
			HalfEdge<P> t0 = new HalfEdge<>(p, f0);
			e0.setTwin(t0);

			f0.setEdge(h0);
			f1.setEdge(h2);

			h1.setFace(f0);
			t0.setFace(f0);
			h0.setFace(f0);

			h2.setFace(f1);
			t1.setFace(f1);
			e0.setFace(f1);

			h0.setNext(h1);
			h1.setNext(t0);
			t0.setNext(h0);

			e0.setNext(h2);
			h2.setNext(t1);
			t1.setNext(e0);
		}
		else {
			h0.getPrevious().setNext(t1);
			t1.setNext(h0);
		}

		if(!o0.isBoundary()) {
			HalfEdge<P> o1 = o0.getNext();
			HalfEdge<P> o2 = o1.getNext();

			P v3 = o1.getEnd();
			Face<P> f2 = new Face<>();

			// face
			HalfEdge<P> e2 = new HalfEdge<>(v3, o0.getFace());
			HalfEdge<P> t2 = new HalfEdge<>(p, f2);
			e2.setTwin(t2);

			f2.setEdge(o1);
			f3.setEdge(o0);

			o1.setFace(f2);
			t2.setFace(f2);
			e1.setFace(f2);

			o2.setFace(f3);
			o0.setFace(f3);
			e2.setFace(f3);

			e1.setNext(o1);
			o1.setNext(t2);
			t2.setNext(e1);

			o0.setNext(e2);
			e2.setNext(o2);
			o2.setNext(o0);
		}
		else {
			e1.setNext(o0.getNext());
			o0.setNext(e1);
		}

		this.face = f0;
	}

	private static <P extends IPoint> Triple<P, P, P> tripleOf(List<HalfEdge<P>> points) {
		assert points.size() == 3;
		return Triple.of(points.get(0).getEnd(), points.get(1).getEnd(), points.get(2).getEnd());
	}

	public void splitEdgeDB(@NotNull P p, @NotNull HalfEdge<P> halfEdge) {

		// 1. gather references
		HalfEdge<P> he = halfEdge;

		// to be legalized edges
		HalfEdge<P> h1 = he.getNext();
		HalfEdge<P> h2 = h1.getNext();

		HalfEdge<P> o1 = he.getTwin().getNext();
		HalfEdge<P> o2 = o1.getNext();

		Face<P> face0 = he.getFace();
		Face<P> face3 = he.getTwin().getFace();

		// 2. do the splitEdge
		splitEdge(p, he);

		// 3. update DB
		DAG<DAGElement<P>> f0Dag = map.get(face0);
		DAG<DAGElement<P>> f3Dag = map.get(face3);

		map.remove(face0);
		map.remove(face3);

		HalfEdge<P> t0 = he.getPrevious();
		Face<P> face1 = t0.getTwin().getFace();

		HalfEdge<P> e2 = he.getTwin().getNext();
		Face<P> face2 = e2.getTwin().getFace();


		DAG<DAGElement<P>> nf0Dag = new DAG<>(new DAGElement(face0, tripleOf(face0.getEdges())));
		DAG<DAGElement<P>> nf1Dag = new DAG<>(new DAGElement(face1, tripleOf(face1.getEdges())));

		DAG<DAGElement<P>> nf2Dag = new DAG<>(new DAGElement(face2, tripleOf(face2.getEdges())));
		DAG<DAGElement<P>> nf3Dag = new DAG<>(new DAGElement(face3, tripleOf(face3.getEdges())));

		f0Dag.addChild(nf0Dag);
		f0Dag.addChild(nf1Dag);

		f3Dag.addChild(nf2Dag);
		f3Dag.addChild(nf3Dag);


		map.put(face0, nf0Dag);
		map.put(face1, nf1Dag);

		map.put(face2, nf2Dag);
		map.put(face3, nf3Dag);

		// 4. legalize result
		legalize(p, h1);
		legalize(p, h2);
		legalize(p, o1);
		legalize(p, o2);
	}


	@Deprecated
	public HalfEdge<P> splitBoth(@NotNull P p, @NotNull DAG<DAGElement<P>> xyzDag, @NotNull DAG<DAGElement<P>> xwzDag) {

		VTriangle xyzTriangle = xyzDag.getElement().getTriangle();
		Face<P> xyzFace = xyzDag.getElement().getFace();
		List<HalfEdge<P>> edges = xyzFace.getEdges();

		HalfEdge<P> xz;
		HalfEdge<P> zy;
		HalfEdge<P> yx;

		// get the edge the point is on.
		if(pointOnEdge(edges.get(0), p) <= eps) {
			xz = edges.get(0);
			zy = edges.get(1);
			yx = edges.get(2);
		}
		else if(pointOnEdge(edges.get(1), p) <= eps) {
			xz = edges.get(1);
			zy = edges.get(2);
			yx = edges.get(0);
		}
		else if(pointOnEdge(edges.get(2), p) <= eps) {
			xz = edges.get(2);
			zy = edges.get(0);
			yx = edges.get(1);
		}
		else {
			throw new IllegalArgumentException(p + " lies on no edge!");
		}

		HalfEdge<P> zx = xz.getTwin();
		HalfEdge<P> xw = zx.getNext();
		HalfEdge<P> wz = xw.getNext();

		Face<P> xwp = new Face<>(xw);
		Face<P> wzp = new Face<>(wz);
		Face<P> zyp = new Face<>(zy);
		Face<P> yxp = new Face<>(yx);

		// update faces for old edges
		xw.setFace(xwp);
		wz.setFace(wzp);
		zy.setFace(zyp);
		yx.setFace(yxp);

		P x = yx.getEnd();
		P y = zy.getEnd();
		P z = wz.getEnd();
		P w = xw.getEnd();

		// new edges with twins
		HalfEdge<P> px = new HalfEdge<>(x, xwp);
		HalfEdge<P> xp = new HalfEdge<>(p, yxp);
		px.setTwin(xp);

		HalfEdge<P> pz = new HalfEdge<>(z, zyp);
		HalfEdge<P> zp = new HalfEdge<>(p, wzp);
		pz.setTwin(zp);

		HalfEdge<P> py = new HalfEdge<>(y, yxp);
		HalfEdge<P> yp = new HalfEdge<>(p, zyp);
		py.setTwin(yp);

		HalfEdge<P> pw = new HalfEdge<>(w, wzp);
		HalfEdge<P> wp = new HalfEdge<>(p, xwp);
		pw.setTwin(wp);

		// build 4 triangles
		xw.setNext(wp);
		wp.setNext(px);
		px.setNext(xw);

		wz.setNext(zp);
		zp.setNext(pw);
		pw.setNext(wz);

		zy.setNext(yp);
		yp.setNext(pz);
		pz.setNext(zy);

		yx.setNext(xp);
		xp.setNext(py);
		py.setNext(yx);

		DAG<DAGElement<P>> zypDag = new DAG<>(new DAGElement(zyp, Triple.of(z, y, p)));
		DAG<DAGElement<P>> yxpDag = new DAG<>(new DAGElement(yxp, Triple.of(y, x, p)));

		DAG<DAGElement<P>> xwpDag = new DAG<>(new DAGElement(xwp, Triple.of(x, w, p)));
		DAG<DAGElement<P>> wzpDag = new DAG<>(new DAGElement(wzp, Triple.of(w, z, p)));

		xyzDag.addChild(zypDag);
		xyzDag.addChild(yxpDag);

		xwzDag.addChild(xwpDag);
		xwzDag.addChild(wzpDag);

		map.remove(xyzDag.getElement().getFace());
		map.remove(xwzDag.getElement().getFace());

		map.put(zyp, zypDag);
		map.put(yxp, yxpDag);

		map.put(xwp, xwpDag);
		map.put(wzp, wzpDag);

		legalize(p, zy);
		legalize(p, yx);
		legalize(p, xw);
		legalize(p, wz);
		return yp;
	}

	private static  <D extends IPoint> double pointOnEdge(final HalfEdge<D> edge, D point) {
		double sign = Math.abs(GeometryUtils.sign(point, edge.getEnd(), edge.getPrevious().getEnd()));
		return sign;
	}

	/**
	 * Splits the triangle xyz into three new triangles xyp, yzp and zxp.
	 *
	 * @param p         the point which splits the triangle
	 * @param xyzDag    the dag which has as its root the face which will be split
	 *
	 * returns a half-edge which has p as its end vertex
	 */
	public HalfEdge<P> splitTriangleDB(@NotNull P p, @NotNull DAG<DAGElement<P>> xyzDag) {
		Face<P> xyzFace = xyzDag.getElement().getFace();

		List<HalfEdge<P>> edges = xyzFace.getEdges();

		Face xyp = new Face();
		Face yzp = new Face();
		Face zxp = new Face();

		HalfEdge<P> zx = edges.get(0);
		HalfEdge<P> xy = edges.get(1);
		HalfEdge<P> yz = edges.get(2);

		P x = zx.getEnd();
		P y = xy.getEnd();
		P z = yz.getEnd();

		HalfEdge<P> yp = new HalfEdge<>(p, xyp);
		HalfEdge<P> py = new HalfEdge<>(y, yzp);
		yp.setTwin(py);

		HalfEdge<P> xp = new HalfEdge<>(p, zxp);
		HalfEdge<P> px = new HalfEdge<>(x, xyp);
		xp.setTwin(px);

		HalfEdge<P> zp = new HalfEdge<>(p, yzp);
		HalfEdge<P> pz = new HalfEdge<>(z, zxp);
		zp.setTwin(pz);

		zx.setNext(xp);
		xp.setNext(pz);
		pz.setNext(zx);

		xy.setNext(yp);
		yp.setNext(px);
		px.setNext(xy);

		yz.setNext(zp);
		zp.setNext(py);
		py.setNext(yz);

		xyp.setEdge(yp);
		yzp.setEdge(py);
		zxp.setEdge(xp);

		xy.setFace(xyp);
		zx.setFace(zxp);
		yz.setFace(yzp);

		DAG<DAGElement<P>> xypDag = new DAG<>(new DAGElement(xyp, Triple.of(x, y, p)));
		DAG<DAGElement<P>> yzpDag = new DAG<>(new DAGElement(yzp, Triple.of(y, z, p)));
		DAG<DAGElement<P>> zxpDag = new DAG<>(new DAGElement(zxp, Triple.of(z, x, p)));

		xyzDag.addChild(xypDag);
		xyzDag.addChild(yzpDag);
		xyzDag.addChild(zxpDag);

		map.remove(xyzDag.getElement().getFace());
		map.put(xyp, xypDag);
		map.put(yzp, yzpDag);
		map.put(zxp, zxpDag);

		legalize(p, zx);
		legalize(p, xy);
		legalize(p, yz);

		this.face = xyp;
		return zp;
	}

	public DAG<DAGElement<P>> getDag() {
		return dag;
	}

	/**
	 * Checks if the edge xy of the triangle xyz is illegal with respect to a point p, which is the case if:
	 * There is a point p and a triangle yxp and p is in the circumscribed cycle of xyz. The assumption is
	 * that the triangle yxp exists.
	 *
	 * @param p     the point that might causes the feasibility problem.
	 * @param edge  the edge that might be illegal
	 * @return true if the edge with respect to p is illegal, otherwise false
	 */
	private boolean isIllegalEdge(P p, HalfEdge<P> edge){
		if(edge.hasTwin() && !edge.getTwin().getFace().isBorder()) {
			P x = edge.getTwin().getEnd();
			P y = edge.getTwin().getNext().getEnd();
			P z = edge.getTwin().getNext().getNext().getEnd();
			VTriangle triangle = new VTriangle(new VPoint(x.getX(), x.getY()), new VPoint(y.getX(), y.getY()), new VPoint(z.getX(), z.getY()));
			return triangle.isInCircumscribedCycle(p);
		}
		return false;
	}

	private boolean isFlipFeasible(final HalfEdge<P> halfEdge){
		return halfEdge.isValid() && !halfEdge.getFace().isBorder();
	}

	/**
	 * Tests if a flip for this half-edge is valid, i.e. the edge does not already exist.
	 *
	 * @param halfEdge the half-edge that might be flipped
	 * @return true if and only if the flip is valid
	 */
	public boolean isFlipOk(final HalfEdge<P> halfEdge) {
		if(halfEdge.getFace().isBorder()) {
			return false;
		}
		else {
			HalfEdge<P> xy = halfEdge;
			HalfEdge<P> yx = halfEdge.getTwin();

			if(xy.getNext().getEnd().equals(yx.getNext().getEnd())) {
				return false;
			}

			for(HalfEdge<P> neigbhour : xy.getNext()) {
				if(neigbhour.getEnd().equals(yx.getNext().getEnd())) {
					return false;
				}
			}
		}

		return true;
	}


	/**
	 * Flips an edge in the triangulation assuming the egdge which will be
	 * created is not jet there.
	 *
	 * @param edge the edge which will be flipped.
	 */
	public void flip(final HalfEdge<P> edge) {
 		assert isFlipOk(edge);

		// 1. gather all the references required
		HalfEdge<P> a0 = edge;
		HalfEdge<P> a1 = a0.getNext();
		HalfEdge<P> a2 = a1.getNext();

		HalfEdge<P> b0 = edge.getTwin();
		HalfEdge<P> b1 = b0.getNext();
		HalfEdge<P> b2 = b1.getNext();

		Face<P> fa = a0.getFace();
		Face<P> fb = b0.getFace();

		if(fb.getEdge().equals(b1)) {
			fb.setEdge(a1);
		}

		if(fa.getEdge().equals(a1)) {
			fa.setEdge(b1);
		}

		a0.setEnd(a1.getEnd());
		b0.setEnd(b1.getEnd());

		a0.setNext(a2);
		a2.setNext(b1);
		b1.setNext(a0);

		b0.setNext(b2);
		b2.setNext(a1);
		a1.setNext(b0);

		a1.setFace(fb);
		b1.setFace(fa);

		this.face = fb;
	}

	/**
	 * Legalizes an edge xy of a triangle xyz where xy is the twin of yx of the triangle
	 * yxp and p is the point on the other side of xy. So legalizing means to flip xy with
	 * zp.
	 *
	 * @param p     p is the point on the other side of zx
	 * @param edge  an edge zx of a triangle xyz
	 */
	private void legalize(final P p, final HalfEdge<P> edge) {
		if(isIllegalEdge(p, edge)) {
			Face<P> f1 = edge.getFace();
			Face<P> f2 = edge.getTwin().getFace();

			flip(edge);

			DAG<DAGElement<P>> f1Dag = map.get(f1);
			DAG<DAGElement<P>> f2Dag = map.get(f2);

			// update DAG
			List<P> points1 = f1.getPoints();
			List<P> points2 = f2.getPoints();
			DAG<DAGElement<P>> yzpDag = new DAG<>(new DAGElement(f1, Triple.of(points1.get(0), points1.get(1), points1.get(2))));
			DAG<DAGElement<P>> xypDag = new DAG<>(new DAGElement(f2, Triple.of(points2.get(0), points2.get(1), points2.get(2))));

			f1Dag.addChild(yzpDag);
			f1Dag.addChild(xypDag);

			f2Dag.addChild(yzpDag);
			f2Dag.addChild(xypDag);

			map.put(f1, yzpDag);
			map.put(f2, xypDag);

			if(edge.getEnd().equals(p)) {
				legalize(p, edge.getPrevious());
				legalize(p, edge.getTwin().getNext());
			}
			else {
				legalize(p, edge.getNext());
				legalize(p, edge.getTwin().getPrevious());
			}
		}
	}

	@Override
	public Iterator<Face<P>> iterator() {
		return new FaceIterator();
	}

	public Stream<Face<P>> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	private class FaceIterator implements Iterator<Face<P>> {

		private LinkedList<Face<P>> facesToVisit;
		private Set<Face<P>> visitedFaces;

		private FaceIterator() {
			facesToVisit = new LinkedList<>();
			facesToVisit.add(face);
			visitedFaces = new HashSet<>();
		}

		@Override
		public boolean hasNext() {
			return !facesToVisit.isEmpty();
		}

		@Override
		public Face<P> next() {
			Face<P> nextFace = facesToVisit.removeFirst();
			visitedFaces.add(nextFace);

			for(HalfEdge<P> he : nextFace) {
				Face<P> twinFace = he.getTwin().getFace();

				if(!visitedFaces.contains(twinFace)) {

					// if the triangulation is finalized there are no border triangles
					if(finalized) {
						facesToVisit.add(twinFace);
					}
					else {
						List<P> points = twinFace.getPoints();
						if(!points.contains(p0) && !points.contains(p1) && !points.contains(p2)) {
							facesToVisit.add(twinFace);
						}
						else {
							visitedFaces.add(twinFace);
						}
					}
				}

				visitedFaces.add(twinFace);
			}

			return nextFace;
		}
	}


	// TODO: the following code can be deleted, this is only for visual checks
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int height = 700;
		int width = 700;
		int max = Math.max(height, width);

		Set<VPoint> points = new HashSet<>();
		/*points.add(new VPoint(20,20));
		points.add(new VPoint(20,40));
		points.add(new VPoint(75,53));
		points.add(new VPoint(80,70));*/

		Random r = new Random();
		for(int i=0; i< 1000; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		long ms = System.currentTimeMillis();
		DelaunayTriangulation<VPoint> bw = new DelaunayTriangulation<>(points, (x, y) -> new VPoint(x, y));
		bw.compute();
		Set<VLine> edges = bw.getEdges();
		edges.addAll(bw.getTriangles().stream().map(triangle -> new VLine(triangle.getIncenter(), triangle.p1)).collect(Collectors.toList()));
		System.out.println(System.currentTimeMillis() - ms);

		JFrame window = new JFrame();
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.setBounds(0, 0, max, max);
		window.getContentPane().add(new Lines(edges, points, max));
		window.setVisible(true);

		ms = System.currentTimeMillis();
		BowyerWatsonSlow<VPoint> bw2 = new BowyerWatsonSlow<VPoint>(points, (x, y) -> new VPoint(x, y));
		bw2.execute();
		Set<VLine> edges2 = bw2.getTriangles().stream()
				.flatMap(triangle -> triangle.getLineStream()).collect(Collectors.toSet());
		System.out.println(System.currentTimeMillis() - ms);

		JFrame window2 = new JFrame();
		window2.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window2.setBounds(0, 0, max, max);
		window2.getContentPane().add(new Lines(edges2, points, max));
		window2.setVisible(true);

		UniformTriangulation<VPoint> uniformTriangulation = new UniformTriangulation<>(0, 0, width, height, 10.0, (x, y) -> new VPoint(x, y));
		uniformTriangulation.compute();
		uniformTriangulation.finalize();
		Set<VLine> edges3 = uniformTriangulation.getEdges();
		
		JFrame window3 = new JFrame();
		window3.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window3.setBounds(0, 0, max, max);
		window3.getContentPane().add(new Lines(edges3, edges3.stream().flatMap(edge -> edge.streamPoints()).collect(Collectors.toSet()), max));
		window3.setVisible(true);
	}

	private static class Lines extends JComponent{
		private Set<VLine> edges;
		private Set<VPoint> points;
		private final int max;

		public Lines(final Set<VLine> edges, final Set<VPoint> points, final int max){
			this.edges = edges;
			this.points = points;
			this.max = max;
		}

		public void paint(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setBackground(Color.white);
			g2.setStroke(new BasicStroke(1.0f));
			g2.setColor(Color.gray);

			edges.stream().forEach(edge -> {
				Shape k = new VLine(edge.getP1().getX(), edge.getP1().getY(), edge.getP2().getX(), edge.getP2().getY());
				g2.draw(k);
			});

			points.stream().forEach(point -> {
				VCircle k = new VCircle(point.getX(), point.getY(), 1.0);
				g2.draw(k);
			});

		}
	}
}
