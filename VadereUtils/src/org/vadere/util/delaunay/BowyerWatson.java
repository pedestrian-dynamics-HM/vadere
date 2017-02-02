package org.vadere.util.delaunay;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.*;

public class BowyerWatson<P extends IPoint> implements Triangulation<P> {

	private final Set<P> points;
	private final PointConstructor<P> pointConstructor;
	private final TriangleConstructor<VPoint> triangleConstructor;
	private P p0;
	private P p1;
	private P p2;
	private DAG<DAGElement<P>> dag;
	private final HashMap<Face<P>, DAG<DAGElement<P>>> map;
	private int count;

	public BowyerWatson(
			final Set<P> points,
			final PointConstructor<P> pointConstructor,
			final TriangleConstructor<VPoint> triangleConstructor) {
		this.points = points;
		this.pointConstructor = pointConstructor;
		this.triangleConstructor = triangleConstructor;
		this.map = new HashMap<>();
		this.count = 0;
	}

	public void init() {
		P p_max = points.stream().reduce(pointConstructor.create(Double.MIN_VALUE, Double.MIN_VALUE), (a, b) -> pointConstructor.create(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY())));
		P p_min = points.stream().reduce(pointConstructor.create(Double.MAX_VALUE, Double.MAX_VALUE), (a, b) -> pointConstructor.create(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY())));
		VRectangle bound = new VRectangle(p_min.getX(), p_min.getY(), p_max.getX()-p_min.getX(), p_max.getY()- p_min.getY());

		double gap = 1.0;
		double max = Math.max(bound.getWidth(), bound.getHeight())*2;
		p0 = pointConstructor.create(bound.getX() - max - gap, bound.getY() - gap);
		p1 = pointConstructor.create(bound.getX() + 2 * max + gap, bound.getY() - gap);
		p2 = pointConstructor.create(bound.getX() + (max+2*gap)/2, bound.getY() + 2 * max + gap);

		Face<P> superTriangle = Face.of(p0, p1, p2);
		this.dag = new DAG<>(new DAGElement<P>(superTriangle, Triple.of(p0, p1, p2), triangleConstructor));
		this.map.put(superTriangle, dag);
	}

	public void execude() {
		// 1. compute a random permutation of the point set/collection.

		int count = 0;
		// 2. insert points
		for(P p : points) {
			insert(p);
			count++;
		}
	}

	@Override
	public Face<P> locate(final IPoint point) {
		Optional<DAG<DAGElement<P>>>  optDag = locatePoint(point).stream().findAny();
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
		return map.values().stream()
				.filter(dagElement -> {
					Triple<P, P, P> triple = dagElement.getElement().getVertices();
					Set<P> pointset = new HashSet<>();
					pointset.add(triple.getLeft());
					pointset.add(triple.getMiddle());
					pointset.add(triple.getRight());
					return !pointset.contains(p0) && !pointset.contains(p1) && !pointset.contains(p2);
				})
				.map(dagElementDAG -> dagElementDAG.getElement().getFace());
	}

	@Override
	public void insert(P point) {
		Set<DAG<DAGElement<P>>> leafs = locatePoint(point);
		assert leafs.size() == 2 ||leafs.size() == 1;
		count++;

		// point is inside a triangle
		if(leafs.size() == 1) {
			split(point, leafs.stream().findAny().get());
		} // point lies on an edge of 2 triangles
		else if(leafs.size() == 2) {
			Iterator<DAG<DAGElement<P>>> it = leafs.iterator();
			splitBoth(point, it.next(), it.next());
		}
		else if(leafs.size() == 0) {
			// problem due numerical calculation.
			System.out.println("numerical error!");
		}
		else {
			Set<DAG<DAGElement<P>>> leafs2 = locatePoint(point);
			System.out.println(leafs2 + " contains " + point);
			throw new IllegalArgumentException("something is wrong here, this should never happen " + leafs.size() + " / " + point + " / " + count);
		}
	}

	@Override
	public void remove(P point) {
		throw new UnsupportedOperationException("not jet implemented.");
	}

	public Collection<VTriangle> getTriangles() {
		return getFaces().stream().map(face -> faceToTriangle(face)).collect(Collectors.toSet());
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

	private Set<DAG<DAGElement<P>>> locatePoint(final IPoint point) {

		Set<DAG<DAGElement<P>>> leafs = new HashSet<>();
		LinkedList<DAG<DAGElement<P>>> nodesToVisit = new LinkedList<>();
		nodesToVisit.add(dag);

		while(!nodesToVisit.isEmpty()) {
			DAG<DAGElement<P>> currentNode = nodesToVisit.removeLast();
			//System.out.println(nodesToVisit.size());
			if(currentNode.getElement().getTriangle().isPartOf(point, 0.0)) {
				if(currentNode.isLeaf()) {
					leafs.add(currentNode);
				}
				else {
					boolean check = false;
					for(DAG<DAGElement<P>> child : currentNode.getChildren()) {
						if(child.getElement().getTriangle().isPartOf(point, 0.0)) {
							check = true;
							nodesToVisit.add(child);
							break;
						}
					}

					// this may (due to nummerical errors) happen if the point is exactly on an edge of 2 triangles => add both
					if(!check) {
						int count = 0;
						for(DAG<DAGElement<P>> child : currentNode.getChildren()) {
							if(child.getElement().getTriangle().isPartOf(point, 0.0)) {
								nodesToVisit.add(child);
								count++;
							}
						}

						if(count != 2) {
							System.out.println("this can not be the case!");
						}
					}
				}
			}
		}

		return leafs;
	}

	public Pair<DAG<DAGElement<P>>, DAG<DAGElement<P>>> splitBoth(@NotNull P p, @NotNull DAG<DAGElement<P>> xyzDag, @NotNull DAG<DAGElement<P>> xwzDag) {
		System.out.println("split both");

		VTriangle xyzTriangle = xyzDag.getElement().getTriangle();
		Face<P> xyzFace = xyzDag.getElement().getFace();
		List<HalfEdge<P>> edges = xyzFace.getEdges();

		HalfEdge<P> xz;
		HalfEdge<P> zy;
		HalfEdge<P> yx;

		double eps = 0.00001;
		if(Math.abs(pointOnEdge(edges.get(0), p)) < eps) {
			xz = edges.get(0);
			zy = edges.get(1);
			yx = edges.get(2);
		}
		else if(Math.abs(pointOnEdge(edges.get(1), p)) < eps) {
			xz = edges.get(1);
			zy = edges.get(2);
			yx = edges.get(0);
		}
		else if(Math.abs(pointOnEdge(edges.get(2), p)) < eps) {
			xz = edges.get(2);
			zy = edges.get(0);
			yx = edges.get(1);
		}
		else {
			pointOnEdge(edges.get(0), p);
			pointOnEdge(edges.get(1), p);
			pointOnEdge(edges.get(2), p);
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

		DAG<DAGElement<P>> zypDag = new DAG<>(new DAGElement(zyp, Triple.of(z, y, p), triangleConstructor));
		DAG<DAGElement<P>> yxpDag = new DAG<>(new DAGElement(yxp, Triple.of(y, x, p), triangleConstructor));

		DAG<DAGElement<P>> xwpDag = new DAG<>(new DAGElement(xwp, Triple.of(x, w, p), triangleConstructor));
		DAG<DAGElement<P>> wzpDag = new DAG<>(new DAGElement(wzp, Triple.of(w, z, p), triangleConstructor));

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
		return Pair.of(xyzDag, xwzDag);
	}

	private static  <D extends IPoint> double pointOnEdge(final HalfEdge<D> edge, D point) {
		double sign = GeometryUtils.sign(point, edge.getEnd(), edge.getPrevious().getEnd());
		return sign;
	}

	/**
	 * Splits the triangle xyz into three new triangles xyp, yzp and zxp.
	 *
	 * @param p
	 * @param xyzDag
	 */
	public DAG<DAGElement<P>> split(@NotNull P p, @NotNull DAG<DAGElement<P>> xyzDag) {
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

		DAG<DAGElement<P>> xypDag = new DAG<>(new DAGElement(xyp, Triple.of(x, y , p), triangleConstructor));
		DAG<DAGElement<P>> yzpDag = new DAG<>(new DAGElement(yzp, Triple.of(y, z, p), triangleConstructor));
		DAG<DAGElement<P>> zxpDag = new DAG<>(new DAGElement(zxp, Triple.of(z, x, p), triangleConstructor));

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

		return xyzDag;
	}

	public DAG<DAGElement<P>> getDag() {
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
			P x = edge.getTwin().getEnd();
			P y = edge.getTwin().getNext().getEnd();
			P z = edge.getTwin().getNext().getNext().getEnd();
			VTriangle triangle = new VTriangle(new VPoint(x.getX(), x.getY()), new VPoint(y.getX(), y.getY()), new VPoint(z.getX(), z.getY()));
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
			HalfEdge<P> xz = zx.getTwin();
			HalfEdge<P> zy = xz.getNext();
			HalfEdge<P> yx = zy.getNext();

			P x = zx.getEnd();
			P y = zy.getEnd();
			P z = pz.getEnd();

			Face<P> f1 = xz.getFace();
			DAG<DAGElement<P>> xpzDag = map.get(f2);
			DAG<DAGElement<P>> xzyDag = map.get(f1);

			// update data structure
			HalfEdge<P> py = new HalfEdge<>(y, f2);
			HalfEdge<P> yp = new HalfEdge<>(p, f1);
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

			if(y.equals(p)) {
				System.out.println("" + y + p);
			}

			// update DAG
			DAG<DAGElement<P>> yzpDag = new DAG<>(new DAGElement(f1, Triple.of(y, z, p), triangleConstructor));
			DAG<DAGElement<P>> xypDag = new DAG<>(new DAGElement(f2, Triple.of(x, y, p), triangleConstructor));

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
		for(int i=0; i< 1000000; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		long ms = System.currentTimeMillis();
		BowyerWatson<VPoint> bw = new BowyerWatson<>(points,
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
		Set<VLine> edges2 = bw2.getTriangles().stream()
				//.map(triangle -> Arrays.asList(GeometryUtils.generateAcuteTriangles(triangle)))
				//.flatMap(list -> list.stream())
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
