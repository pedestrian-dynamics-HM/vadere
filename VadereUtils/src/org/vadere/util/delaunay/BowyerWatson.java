package org.vadere.util.delaunay;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.data.DAG;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.*;

public class BowyerWatson<P extends VPoint, T extends VTriangle> {

	private final Collection<P> points;
	private final PointConstructor<P> pointConstructor;
	private final TriangleConstructor<P, T> triangleConstructor;
	private P p0;
	private P p1;
	private P p2;
	private DAG<DAGElement<P, T>> dag;
	private final HashMap<Face<P>, DAG<DAGElement<P, T>>> map;

	public BowyerWatson(
			final Collection<P> points,
			final PointConstructor<P> pointConstructor,
			final TriangleConstructor<P, T> triangleConstructor) {
		this.points = points;
		this.pointConstructor = pointConstructor;
		this.triangleConstructor = triangleConstructor;
		this.map = new HashMap<>();
	}

	public void init() {
		P p_max = points.parallelStream().reduce(pointConstructor.create(Double.MIN_VALUE, Double.MIN_VALUE), (a, b) -> pointConstructor.create(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY())));
		P p_min = points.parallelStream().reduce(pointConstructor.create(Double.MIN_VALUE, Double.MIN_VALUE), (a, b) -> pointConstructor.create(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY())));
		VRectangle bound = new VRectangle(p_min.getX(), p_min.getY(), p_max.getX()-p_min.getX(), p_max.getY()- p_min.getY());

		double gap = 1.0;
		double max = Math.max(bound.getWidth(), bound.getHeight());
		p0 = pointConstructor.create(bound.getX() - max - gap, bound.getY() - gap);
		p1 = pointConstructor.create(bound.getX() + 2 * max + gap, bound.getY() - gap);
		p2 = pointConstructor.create(bound.getX() + (max+2*gap)/2, bound.getY() + 2 * max+ gap);

		Face superTriangle = Face.of(p0, p1, p2);
		this.dag = new DAG<>(new DAGElement<>(superTriangle, Triple.of(p0, p1, p2), triangleConstructor));
		this.map.put(superTriangle, dag);
	}

	public void execude() {
		// 1. compute a random permutation of the point set/collection.

		for(P p : points) {
			// find triangle containing p using the DAG.
			Optional<DAG<DAGElement<P, T>>> opt = dag.matchAll(dagElement -> dagElement.getTriangle().contains(p));
			assert opt.isPresent();

			DAG<DAGElement<P, T>> trianglePair = opt.get();
			T vTriangle = trianglePair.getRoot().getTriangle();
			Face<P> fTriangle = trianglePair.getRoot().getFace();

			// p lies in the interior of the triangle
			if(vTriangle.contains(p)){
				// split the triangle into 3 new triangles.
				split(p, trianglePair);
			}
			else {
				throw new IllegalStateException("this should never happen!");
			}
		}
	}

	public Collection<T> getTriangles() {
		return map.values().stream().map(dagElement -> dagElement.getRoot().getTriangle()).collect(Collectors.toList());
	}

	public Set<VLine> getEdges() {
		return map.values().stream().flatMap(dagElement -> dagElement.getRoot().getTriangle().getLineStream()).collect(Collectors.toSet());
	}

	/**
	 * Splits the triangle xyz into three new triangles xyp, yzp and zxp.
	 *
	 * @param p
	 * @param xyzDag
	 */
	public DAG<DAGElement<P, T>> split(@NotNull P p, @NotNull DAG<DAGElement<P, T>> xyzDag) {
		VTriangle xyzTriangle = xyzDag.getRoot().getTriangle();
		Face<P> xyzFace = xyzDag.getRoot().getFace();

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

		HalfEdge<P> yp = new HalfEdge<P>(p, xyp);
		HalfEdge<P> py = new HalfEdge<P>(y, yzp);
		yp.setTwin(py);

		HalfEdge<P> xp = new HalfEdge<P>(p, zxp);
		HalfEdge<P> px = new HalfEdge<P>(x, xyp);
		xp.setTwin(px);

		HalfEdge<P> zp = new HalfEdge<P>(p, yzp);
		HalfEdge<P> pz = new HalfEdge<P>(z, zxp);
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

		DAG<DAGElement<P, T>> xypDag = new DAG<>(new DAGElement(xyp, Triple.of(x, y , p), triangleConstructor));
		DAG<DAGElement<P, T>> yzpDag = new DAG<>(new DAGElement(yzp, Triple.of(y, z, p), triangleConstructor));
		DAG<DAGElement<P, T>> zxpDag = new DAG<>(new DAGElement(zxp, Triple.of(z, x, p), triangleConstructor));

		xyzDag.addChild(xypDag);
		xyzDag.addChild(yzpDag);
		xyzDag.addChild(zxpDag);

		map.remove(xyzDag.getRoot().getFace());
		map.put(xyp, xypDag);
		map.put(yzp, yzpDag);
		map.put(zxp, zxpDag);

		legalize(p, zx);
		legalize(p, xy);
		legalize(p, yz);

		return xyzDag;
	}

	public DAG<DAGElement<P, T>> getDag() {
		return dag;
	}

	/**
	 * Checks if the edge xy of the triangle xyz is illegal, which is the case if:
	 * There is a point p and a triangle yxp and p is in the circumscribed cycle of xyz.
	 *
	 * @param p
	 * @param edge
	 * @return
	 */
	private boolean isIllegalEdge(P p, HalfEdge<P> edge){
		if(edge.hasTwin()) {
			P x = edge.getTwin().get().getEnd();
			P y = edge.getTwin().get().getNext().getEnd();
			P z = edge.getTwin().get().getNext().getNext().getEnd();
			VTriangle triangle = new VTriangle(x,y,z);
			return triangle.isInCircumscribedCycle(p);
		}
		return false;
	}

	/**
	 * Legalizes an edge xy of a triangle xyz where xy is the twin of yx of the triangle
	 * yxp and p is the point on the other side of xy. So legalizing means to flip xy with
	 * zp.
	 *
	 * @param p     p is the point on the other side of xy
	 * @param edge  an edge xy of a triangle xyz
	 */
	private void legalize(final P p, final HalfEdge<P> edge) {
		if(isIllegalEdge(p, edge)) {
			// read all informations
			HalfEdge<P> zx = edge;
			HalfEdge<P> xp = zx.getNext();
			HalfEdge<P> pz = xp.getNext();

			Face<P> f2 = zx.getFace();
			HalfEdge<P> xz = zx.getTwin().get();
			HalfEdge<P> zy = xz.getNext();
			HalfEdge<P> yx = zy.getNext();

			P x = zx.getEnd();
			P y = zy.getEnd();
			P z = pz.getEnd();

			Face<P> f1 = xz.getFace();
			DAG<DAGElement<P, T>> xpzDag = map.get(f2);
			DAG<DAGElement<P, T>> xzyDag = map.get(f1);

			// update data structure
			HalfEdge<P> py = new HalfEdge<P>(y, f2);
			HalfEdge<P> yp = new HalfEdge<P>(p, f1);
			py.setTwin(yp);

			xp.setNext(py);
			py.setNext(yx);
			yx.setNext(xp);

			xp.setFace(f2);
			py.setFace(f2);
			yx.setFace(f2);
			f2.setEdge(py);


			pz.setNext(zy);
			zy.setNext(yp);
			yp.setNext(pz);
			f1.setEdge(yp);

			pz.setFace(f1);
			zy.setFace(f1);
			yp.setFace(f1);

			// update DAG
			DAG<DAGElement<P, T>> yzpDag = new DAG<>(new DAGElement(f1, Triple.of(y, z, p), triangleConstructor));
			DAG<DAGElement<P, T>> xypDag = new DAG<>(new DAGElement(f2, Triple.of(x, y, p), triangleConstructor));

			xpzDag.addChild(yzpDag);
			xpzDag.addChild(xypDag);

			xzyDag.addChild(yzpDag);
			xzyDag.addChild(xypDag);

			map.put(f1, yzpDag);
			map.put(f2, xypDag);

			legalize(p, py.getNext());
			legalize(p, yp.getPrevious());
		}
	}

	/*public class DAGElement {
		private Face<P> face;
		private Triple<P, P, P> vertices;
		private T triangle;

		public DAGElement(final Face<P> face, final Triple<P, P, P> vertices) {
			this.face = face;
			this.vertices = vertices;
			this.triangle = triangleConstructor.create(vertices.getLeft(), vertices.getMiddle(), vertices.getRight());
		}

		public Face<P> getFace() {
			return face;
		}

		public T getTriangle() {
			return triangle;
		}

		public Triple<P, P, P> getVertices() {
			return vertices;
		}
	}*/

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
		for(int i=0; i<100; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		long ms = System.currentTimeMillis();
		BowyerWatson<VPoint, VTriangle> bw = new BowyerWatson<>(points,
				(x, y) -> new VPoint(x, y),
				(p1, p2, p3) -> new VTriangle(p1, p2, p3));
		bw.init();
		bw.execude();
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
		Set<VLine> edges2 = bw2.getVTriangles().stream()
				.map(triangle -> Arrays.asList(GeometryUtils.generateAcuteTriangles(triangle)))
				.flatMap(list -> list.stream())
				.flatMap(triangle -> triangle.getLineStream()).collect(Collectors.toSet());
		System.out.println(System.currentTimeMillis() - ms);



		JFrame window2 = new JFrame();
		window2.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window2.setBounds(0, 0, max, max);
		window2.getContentPane().add(new Lines(edges2, points, max));
		window2.setVisible(true);
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
