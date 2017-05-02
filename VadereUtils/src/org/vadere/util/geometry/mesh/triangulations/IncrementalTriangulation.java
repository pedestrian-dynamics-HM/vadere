package org.vadere.util.geometry.mesh.triangulations;

import org.apache.commons.collections.IteratorUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.iterators.FaceIterator;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.iterators.AdjacentFaceIterator;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.impl.PMesh;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.triangulation.BowyerWatsonSlow;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.*;

public class IncrementalTriangulation<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulation<P, E, F> {

	protected Set<P> points;

	// TODO: use symbolic for the super-triangle points instead of real points!
	private P p0;
	private P p1;
	private P p2;
	private E he0;
	private E he1;
	private E he2;
	private final VRectangle bound;
	private boolean finalized = false;
	private IMesh<P, E, F> mesh;
	private IPointLocator<P, E, F> pointLocator;
	private boolean initialized = false;

	// TODO this epsilon it hard coded!!! => replace it with a user choice
	private double epsilon = 0.0001;

	protected F superTriangle;
	private F borderFace;
	private final Predicate<E> illegalPredicate;
	private static Logger log = LogManager.getLogger(IncrementalTriangulation.class);

	public IncrementalTriangulation(
			final Set<P> points,
			final Predicate<E> illegalPredicate) {
		this.points = points;
		this.illegalPredicate = illegalPredicate;
		this.bound = GeometryUtils.bound(points);
	}

	public IncrementalTriangulation(final Set<P> points) {
		this(points, halfEdge -> true);
	}

	public IncrementalTriangulation(
			final VRectangle bound,
			final Predicate<E> illegalPredicate) {
		this.points = new HashSet<>();
		this.illegalPredicate = illegalPredicate;
		this.bound = bound;
	}

	public IncrementalTriangulation(final VRectangle bound) {
		this(bound, halfEdge -> true);
	}

	public void setPointLocator(final IPointLocator<P, E, F> pointLocator) {
		this.pointLocator = pointLocator;
	}

	public void setMesh(final IMesh<P, E, F> mesh) {
		this.mesh = mesh;
	}

	public F getSuperTriangle() {
		return superTriangle;
	}

	@Override
	public void init() {
		double gap = 1.0;
		double max = Math.max(bound.getWidth(), bound.getHeight())*2;
		p0 = mesh.insertVertex(bound.getX() - max - gap, bound.getY() - gap);
		p1 = mesh.insertVertex(bound.getX() + 2 * max + gap, bound.getY() - gap);
		p2 = mesh.insertVertex(bound.getX() + (max+2*gap)/2, bound.getY() + 2 * max + gap);

		superTriangle = mesh.createFace(p0, p1, p2);
		borderFace = mesh.getTwinFace(mesh.getEdge(superTriangle));

		List<E> borderEdges = mesh.getEdges(borderFace);
		he0 = borderEdges.get(0);
		he1 = borderEdges.get(1);
		he2 = borderEdges.get(2);

		this.finalized = false;
		this.initialized = true;
	}


	@Override
	public void compute() {
		init();

		// 1. insert points
		for(P p : points) {
			insert(p);
		}

		// 2. remove super triangle
		finalize();
	}

	@Override
	public E insert(P point) {

		Collection<F> faces = this.pointLocator.locatePoint(point, true);
		int numberOfFaces = faces.size();

		// problem due numerical calculation.
		faces = faces.stream().filter(f -> !getMesh().isMember(f, point.getX(), point.getY(), epsilon)).collect(Collectors.toList());
		E insertedEdge = null;

		if(numberOfFaces == 0) {
			log.warn("no face found at " + point);
		}
		else if(faces.size() == 1) {
			log.info("splitTriangle:" + point);
			F face = faces.iterator().next();
			splitTriangle(face, point,  true);
			insertedEdge = mesh.getEdge(point);
			insertEvent(insertedEdge);

		} // point lies on an edge of 2 triangles
		else if(faces.size() == 2) {
			Iterator<F> it = faces.iterator();
			log.info("splitEdge:" + point);
			E halfEdge = findTwins(it.next(), it.next()).get();
			List<E> newEdges = splitEdge(point, halfEdge, true);
			insertedEdge = newEdges.stream().filter(he -> getMesh().getVertex(he).equals(point)).findAny().get();
			insertEvent(insertedEdge);
		}
		else if(faces.size() == 0) {
			log.warn("ignore insertion point, since the point " + point + " already exists or it is too close to another point!");
		}
		else {
			log.error("more than 2 faces found and the point " + point + " is not a vertex jet, there is something wrong with the mesh structure!");
		}

		return insertedEdge;
	}

	@Override
	public void insert(final Set<P> points) {
		if(!initialized) {
			init();
		}

		// 1. insert points
		for(P p : points) {
			insert(p);
		}
	}

	/**
	 * Removes the super triangle from the mesh data structure.
	 */
	public void finalize() {
		if(!finalized) {
			// we have to use other halfedges than he1 and he2 since they might be deleted
			// if we deleteBoundaryFace he0!
			List<F> faces1 = IteratorUtils.toList(new AdjacentFaceIterator(mesh, he0));
			List<F> faces2 = IteratorUtils.toList(new AdjacentFaceIterator(mesh, he1));
			List<F> faces3 = IteratorUtils.toList(new AdjacentFaceIterator(mesh, he2));

			faces1.removeIf(f -> mesh.isBoundary(f));
			faces1.forEach(f -> deleteBoundaryFace(f));

			faces2.removeIf(f -> mesh.isDestroyed(f) || mesh.isBoundary(f));
			faces2.forEach(f -> deleteBoundaryFace(f));

			faces3.removeIf(f -> mesh.isDestroyed(f) || mesh.isBoundary(f));
			faces3.forEach(f -> deleteBoundaryFace(f));

			finalized = true;
		}
	}

	public boolean isDeletionOk(final F face) {
		if(mesh.isDestroyed(face)) {
			return false;
		}

		for(E halfEdge : mesh.getEdgeIt(face)) {
			if(mesh.isBoundary(mesh.getTwin(halfEdge))) {
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
	public void deleteBoundaryFace(final F face) {
		assert isDeletionOk(face);

		// 3 cases: 1. triangle consist of 1, 2 or 3 boundary edges
		List<E> boundaryEdges = new ArrayList<>(3);
		List<E> nonBoundaryEdges = new ArrayList<>(3);

		for(E halfEdge : mesh.getEdgeIt(face)) {
			if(mesh.isBoundary(mesh.getTwin(halfEdge))) {
				boundaryEdges.add(halfEdge);
			}
			else {
				nonBoundaryEdges.add(halfEdge);
			}
		}

		if(boundaryEdges.size() == 3) {
			// release memory
			mesh.getEdges(face).forEach(halfEdge -> mesh.destroyEdge(halfEdge));
		}
		else if(boundaryEdges.size() == 2) {
			E toB = mesh.isBoundary(mesh.getTwin(mesh.getNext(boundaryEdges.get(0)))) ? boundaryEdges.get(0) : boundaryEdges.get(1);
			E toF = mesh.isBoundary(mesh.getTwin(mesh.getNext(boundaryEdges.get(0)))) ? boundaryEdges.get(1) : boundaryEdges.get(0);
			E nB = nonBoundaryEdges.get(0);
			mesh.setFace(nB, mesh.getTwinFace(toF));
			mesh.setNext(nB, mesh.getNext(mesh.getTwin(toB)));
			mesh.setPrev(nB, mesh.getPrev(mesh.getTwin(toF)));
			mesh.setEdge(mesh.getFace(mesh.getTwin(toF)), nB);

			//this.face = mesh.getTwinFace(toF);

			// release memory
			mesh.destroyEdge(toF);
			mesh.destroyEdge(toB);

		}
		else {
			E boundaryHe = boundaryEdges.get(0);
			E prec = mesh.getPrev(mesh.getTwin(boundaryHe));
			E succ = mesh.getNext(mesh.getTwin(boundaryHe));

			E next = mesh.getNext(boundaryHe);
			E prev = mesh.getPrev(boundaryHe);
			mesh.setPrev(next, prec);
			mesh.setFace(next, mesh.getTwinFace(boundaryHe));

			mesh.setNext(prev, succ);
			mesh.setFace(prev, mesh.getTwinFace(boundaryHe));

			mesh.setEdge(mesh.getFace(prec), prec);

			//this.face = mesh.getFace(prec);
			// release memory
			mesh.destroyEdge(boundaryHe);
		}

		mesh.destroyFace(face);
	}

	@Override
	public IMesh<P, E, F> getMesh() {
		return mesh;
	}

	@Override
	public Optional<F> locate(final P point) {
		return pointLocator.locate(point);
	}

	@Override
	public Set<F> getFaces() {
		return streamFaces().collect(Collectors.toSet());
	}

	@Override
	public Stream<F> streamFaces() {
		return stream();
	}

	@Override
	public Stream<VTriangle> streamTriangles() {
		return stream().map(f -> getMesh().toTriangle(f));
	}

	@Override
	public void remove(P point) {
		throw new UnsupportedOperationException("not jet implemented.");
	}

	public Collection<VTriangle> getTriangles() {
		return stream().map(face -> faceToTriangle(face)).collect(Collectors.toSet());
	}

	public Set<VLine> getEdges() {
		return getTriangles().stream().flatMap(triangle -> triangle.getLineStream()).collect(Collectors.toSet());
	}

	private VTriangle faceToTriangle(final F face) {
		List<P> points = mesh.getEdges(face).stream().map(edge -> mesh.getVertex(edge)).collect(Collectors.toList());
		P p1 = points.get(0);
		P p2 = points.get(1);
		P p3 = points.get(2);
		return new VTriangle(new VPoint(p1.getX(), p1.getY()), new VPoint(p2.getX(), p2.getY()), new VPoint(p3.getX(), p3.getY()));
	}


	/**
	 * Checks if the edge xy of the triangle xyz is illegal with respect to a point p, which is the case if:
	 * There is a point p and a triangle yxp and p is in the circumscribed cycle of xyz. The assumption is
	 * that the triangle yxp exists.
	 *
	 * @param edge  the edge that might be illegal
	 * @return true if the edge with respect to p is illegal, otherwise false
	 */
	@Override
	public boolean isIllegal(E edge) {
		return isIllegal(edge, mesh);
	}

	public static <P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> boolean isIllegal(E edge, IMesh<P, E, F> mesh) {
		if(!mesh.isBoundary(mesh.getTwinFace(edge))) {
			P p = mesh.getVertex(mesh.getNext(edge));
			E t0 = mesh.getTwin(edge);
			E t1 = mesh.getNext(t0);
			E t2 = mesh.getNext(t1);

			P x = mesh.getVertex(t0);
			P y = mesh.getVertex(t1);
			P z = mesh.getVertex(t2);

			VTriangle triangle = new VTriangle(new VPoint(x), new VPoint(y), new VPoint(z));
			return triangle.isInCircumscribedCycle(p);
		}
		return false;
	}

	/*public static <P extends IPoint> boolean isIllegalEdge(final E edge){
		P p = edge.getNext().getEnd();

		if(!edge.isAtBoundary() && !edge.getTwin().isAtBoundary()) {
			P x = edge.getTwin().getEnd();
			P y = edge.getTwin().getNext().getEnd();
			P z = edge.getTwin().getNext().getNext().getEnd();
			VTriangle triangle = new VTriangle(new VPoint(x.getX(), x.getY()), new VPoint(y.getX(), y.getY()), new VPoint(z.getX(), z.getY()));
			return triangle.isInCircumscribedCycle(p);
		}
		return false;
	}*/

	@Override
	public void flipEdgeEvent(final F f1, final F f2) {
		pointLocator.flipEdgeEvent(f1, f2);
	}

	@Override
	public void splitFaceEvent(final F original, final F[] faces) {
		pointLocator.splitFaceEvent(original, faces);
	}

	@Override
	public void insertEvent(final E halfEdge) {
		pointLocator.insertEvent(halfEdge);
	}

	@Override
	public Iterator<F> iterator() {
		return new FaceIterator(mesh);
	}

	public Stream<F> stream() {
		return StreamSupport.stream(this.spliterator(), false);
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

		IPointConstructor<VPoint> pointConstructor =  (x, y) -> new VPoint(x, y);
		long ms = System.currentTimeMillis();

		PMesh<VPoint> mesh = new PMesh<>(pointConstructor);
		IncrementalTriangulation<VPoint, PHalfEdge<VPoint>, PFace<VPoint>> bw = new IncrementalTriangulation<>(points);
		bw.setMesh(mesh);
		bw.setPointLocator(new DelaunayTree<>(bw));
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

		UniformTriangulation<VPoint, PHalfEdge<VPoint>, PFace<VPoint>> uniformTriangulation = ITriangulation.createUnifirmTriangulation(
				IPointLocator.Type.DELAUNAY_TREE,
				new VRectangle(0, 0, width, height),
				10.0,
				(x, y) -> new VPoint(x, y));

		uniformTriangulation.compute();
		Set<VLine> edges3 = uniformTriangulation.getEdges();

		JFrame window3 = new JFrame();
		window3.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window3.setBounds(0, 0, max, max);
		window3.getContentPane().add(new Lines(edges3, edges3.stream().flatMap(edge -> edge.streamPoints()).collect(Collectors.toSet()), max));
		window3.setVisible(true);

		UniformRefinementTriangulation<VPoint> uniformRefinement = new UniformRefinementTriangulation<>(
				new VRectangle(0, 0, width, height),
				Arrays.asList(new VRectangle(200, 200, 100, 200)),
				p -> 10.0,
				(x, y) -> new VPoint(x, y));

		uniformRefinement.compute();
		Set<VLine> edges4 = uniformRefinement.getEdges();

		JFrame window4 = new JFrame();
		window4.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window4.setBounds(0, 0, max, max);
		window4.getContentPane().add(new Lines(edges4, edges4.stream().flatMap(edge -> edge.streamPoints()).collect(Collectors.toSet()), max));
		window4.setVisible(true);
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
			g2.setColor(Color.black);
			g2.draw(new VRectangle(200, 200, 100, 200));
			g2.setColor(Color.gray);
			//g2.translate(200, 200);
			//g2.scale(0.2, 0.2);

			g2.draw(new VRectangle(200, 200, 100, 200));

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
