package org.vadere.util.triangulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.mesh.iterators.FaceIterator;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
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

public class IncrementalTriangulation<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulation<P, V, E, F> {

	protected Collection<P> points;

	// TODO: use symbolic for the super-triangle points instead of real points!
	private V p0;
	private V p1;
	private V p2;
    private V p3;

	private final VRectangle bound;
	private boolean finalized = false;
	private IMesh<P, V, E, F> mesh;
	private IPointLocator<P, V, E, F> pointLocator;
	private boolean initialized = false;
	private List<V> virtualVertices;
	private int maxDepth = 0;

	private static double BUFFER_PERCENTAGE = 0.01;

	// TODO this epsilon it hard coded!!! => replace it with a user choice
	private double epsilon = 0.0001;
	private double edgeCoincidenceTolerance = 0.0001;

	private F borderFace;
	private final Predicate<E> illegalPredicate;
	private static Logger log = LogManager.getLogger(IncrementalTriangulation.class);

	// constructors using the triangulation without a triangulator
	public IncrementalTriangulation(
			final Collection<P> points,
			final Predicate<E> illegalPredicate) {
		this.points = points;
		this.illegalPredicate = illegalPredicate;
		this.bound = GeometryUtils.bound(points);
		this.finalized = false;
	}

	public IncrementalTriangulation(final Collection<P> points) {
		this(points, halfEdge -> true);
	}

	public IncrementalTriangulation(
			final VRectangle bound,
			final Predicate<E> illegalPredicate) {
		this.points = new HashSet<>();
		this.illegalPredicate = illegalPredicate;
		this.bound = bound;
		this.finalized = false;
	}

    // constructors using the triangulation with a triangulator
	public IncrementalTriangulation(final VRectangle bound) {
		this(bound, halfEdge -> true);
	}

	// end constructors

	public void setPointLocator(final IPointLocator<P, V, E, F> pointLocator) {
		this.pointLocator = pointLocator;
	}

	public void setMesh(final IMesh<P, V, E, F> mesh) {
		this.mesh = mesh;
	}

	@Override
	public void init() {
		if(!initialized) {
            double max = Math.max(bound.getWidth(), bound.getHeight());
		    double epsilon = BUFFER_PERCENTAGE * max; // 1% gap

            double xMin = bound.getMinX() - epsilon;
            double yMin = bound.getMinY() - epsilon;

            double xMax = bound.getMaxX() + epsilon;
            double yMax = bound.getMaxY() + epsilon;


			p0 = mesh.insertVertex(xMin, yMin);
			p1 = mesh.insertVertex(xMax, yMin);
			p2 = mesh.insertVertex(xMin, yMax);
			p3 = mesh.insertVertex(xMax, yMax);

			// counter clockwise!
			F square = mesh.createFace(p0, p1, p3, p2);
			F tri = mesh.createFace(false);

			// start divide the square into 2 triangles
			E edge = mesh.createEdge(p1);
			E twin = mesh.createEdge(p2);

			mesh.setTwin(edge, twin);

			E start = mesh.getEdge(p2);
			if(mesh.isBoundary(start)) {
                start = mesh.getPrev(mesh.getTwin(start));
            }

            E next = mesh.getNext(start);
			E prev = mesh.getPrev(start);
			E nnext = mesh.getNext(next);

            mesh.setPrev(edge, start);
            mesh.setNext(edge, prev);

            mesh.setNext(twin, next);
            mesh.setPrev(twin, nnext);

            mesh.setFace(edge, square);
            mesh.setFace(twin, tri);
            mesh.setFace(mesh.getNext(twin), tri);
            mesh.setFace(mesh.getPrev(twin), tri);

			borderFace = mesh.getTwinFace(mesh.getEdge(square));
			mesh.setEdge(borderFace, mesh.getTwin(start));

            mesh.setEdge(tri, twin);
            mesh.setEdge(square, edge);
            // end divide the square into 2 triangles

			this.virtualVertices = Arrays.asList(p0, p1, p2, p3);
			this.initialized = true;
		}
		else {
			log.warn("the second initialization of the " + this.getClass().getSimpleName() + " has no effect.");
		}
	}


	@Override
	public List<V> getVirtualVertices() {
		return virtualVertices;
	}


	@Override
	public void compute() {
		init();

		// 1. insert points
		for(P p : points) {
			insert(p);
		}

		// 2. remove super triangle
		finish();
	}

    @Override
    public void recompute() {
        initialized = false;
        finalized = false;
        compute();
    }

    @Override
	public E insert(@NotNull P point, @NotNull F face) {
		if(!initialized) {
			init();
		}

		E edge = mesh.closestEdge(face, point.getX(), point.getY());
		P p1 = mesh.getPoint(mesh.getPrev(edge));
		P p2 = mesh.getPoint(edge);

		/*
		 * 3 Cases:
		 *      1) point lies on an vertex of a face => ignore the point
		 *      2) point lies on an edge of a face => split the edge
		 *      3) point lies in the interior of the face => split the face (this should be the main case)
		 */
		if(isMember(face, point.getX(), point.getY(), edgeCoincidenceTolerance)) {
			log.info("ignore insertion point, since the point " + point + " already exists or it is too close to another point!");
			return edge;
		}
		if(GeometryUtils.isOnEdge(p1, p2, point, edgeCoincidenceTolerance)) {
			//log.info("splitEdge()");
			E newEdge = getAnyEdge(splitEdge(point, edge, true));
			insertEvent(newEdge);
			return newEdge;
		}
		else {
			//log.info("splitTriangle()");
			E newEdge = splitTriangle(face, point,  true);
			insertEvent(newEdge);
			return newEdge;
		}
	}

	@Override
	public E insert(P point) {
		F face = this.pointLocator.locatePoint(point, true);
		return insert(point, face);
	}

	@Override
	public void insert(final Collection<P> points) {
		if(!initialized) {
			init();
		}

		// 1. insert points
		for(P p : points) {
			insert(p);
		}
	}

	protected IPointLocator<P, V, E, F> getPointLocator() {
	    return pointLocator;
    }

	/**
	 * Removes the super triangle from the mesh data structure.
	 */
	/*public void finish() {
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
	}*/

	@Override
	public void finish() {
		if(!finalized) {
			// we have to use other halfedges than he1 and he2 since they might be deleted
			// if we deleteBoundaryFace he0!

            for(V virtualPoint : virtualVertices) {
                if(!mesh.isDestroyed(virtualPoint)) {
                    List<F> faces1 = mesh.getFaces(virtualPoint);
                    faces1.removeIf(f -> mesh.isBoundary(f));
                    faces1.forEach(f -> removeFace(f, true));
                }
            }

			/*if(!mesh.isDestroyed(p1)) {
				List<F> faces2 = mesh.getFaces(p1);
				faces2.removeIf(f -> mesh.isDestroyed(f) || mesh.isBoundary(f));
				faces2.forEach(f -> removeFace(f, true));
			}

			if(!mesh.isDestroyed(p2)) {
				List<F> faces3 = mesh.getFaces(p2);
				faces3.removeIf(f -> mesh.isDestroyed(f) || mesh.isBoundary(f));
				faces3.forEach(f -> removeFace(f, true));
			}*/

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
	 * Deletes a face assuming that the face triangleContains at least one boundary edge, otherwise the
	 * deletion will not result in an feasibly triangulation.
	 *
	 * @param face the face that will be deleted, which as to be adjacent to the boundary.
	 */
	public void deleteBoundaryFace(final F face) {
		//assert isDeletionOk(face);

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
	public IMesh<P, V, E, F> getMesh() {
		return mesh;
	}

	@Override
	public Optional<F> locateFace(final P point) {
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

	private VTriangle faceToTriangle(final F face) {
		List<V> points = mesh.getEdges(face).stream().map(edge -> mesh.getVertex(edge)).collect(Collectors.toList());
		V p1 = points.get(0);
		V p2 = points.get(1);
		V p3 = points.get(2);
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
	public boolean isIllegal(E edge, V p) {
		if(!mesh.isAtBoundary(edge) && illegalPredicate.test(edge)) {
			//assert mesh.getVertex(mesh.getNext(edge)).equals(p);
			//V p = mesh.getVertex(mesh.getNext(edge));
			E t0 = mesh.getTwin(edge);
			E t1 = mesh.getNext(t0);
			E t2 = mesh.getNext(t1);

			V x = mesh.getVertex(t0);
			V y = mesh.getVertex(t1);
			V z = mesh.getVertex(t2);

			//return GeometryUtils.angle(x, y, z) + GeometryUtils.angle(x, p, z) > Math.PI;

			//return GeometryUtils.isInCircumscribedCycle(x, y, z, p);
			//if(GeometryUtils.ccw(z,x,y) > 0) {
			return GeometryUtils.isInsideCircle(z, x, y, p);
			//}
			//else {
			//	return GeometryUtils.isInsideCircle(x, z, y, p);
			//}
		}

		return false;
		//return isIllegal(edge, p, mesh);
	}

	/*public static <P extends IPoint, V extends  IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> boolean isIllegal(E edge, V p, IMesh<P, V, E, F> mesh) {
		if(!mesh.isBoundary(mesh.getTwinFace(edge))) {
			//V p = mesh.getVertex(mesh.getNext(edge));
			E t0 = mesh.getTwin(edge);
			E t1 = mesh.getNext(t0);
			E t2 = mesh.getNext(t1);

			V x = mesh.getVertex(t0);
			V y = mesh.getVertex(t1);
			V z = mesh.getVertex(t2);

			//return GeometryUtils.angle(x, y, z) + GeometryUtils.angle(x, p, z) > Math.PI;

			//return GeometryUtils.isInCircumscribedCycle(x, y, z, p);
			if (GeometryUtils.ccw(x,y,z) > 0
					t.dest().rightOf(e) && v.isInCircle(e.orig(), t.dest(), e.dest())) {
			log.info(GeometryUtils.ccw(x,y,z) > 0);
			return GeometryUtils.isInsideCircle(x, y, z, p);
		}
		return false;
	}*/

	public static <P extends IPoint, V extends  IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> boolean isIllegal(E edge, V p, IMesh<P, V, E, F> mesh) {
		if(!mesh.isBoundary(mesh.getTwinFace(edge))) {
			//assert mesh.getVertex(mesh.getNext(edge)).equals(p);
			//V p = mesh.getVertex(mesh.getNext(edge));
			E t0 = mesh.getTwin(edge);
			E t1 = mesh.getNext(t0);
			E t2 = mesh.getNext(t1);

			V x = mesh.getVertex(t0);
			V y = mesh.getVertex(t1);
			V z = mesh.getVertex(t2);

			//return GeometryUtils.angle(x, y, z) + GeometryUtils.angle(x, p, z) > Math.PI;

			//return GeometryUtils.isInCircumscribedCycle(x, y, z, p);
			//if(GeometryUtils.ccw(z,x,y) > 0) {
				return GeometryUtils.isInsideCircle(z, x, y, p);
			//}
			//else {
			//	return GeometryUtils.isInsideCircle(x, z, y, p);
			//}
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
	public void legalizeNonRecursive(@NotNull final E edge, final V p) {
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

	@Override
	public void flipEdgeEvent(final F f1, final F f2) {
		pointLocator.flipEdgeEvent(f1, f2);
	}

	@Override
	public void splitTriangleEvent(final F original, final F f1, F f2, F f3) {
		pointLocator.splitTriangleEvent(original, f1, f2, f3);
	}

	@Override
	public void splitEdgeEvent(F original, F f1, F f2) {
		pointLocator.splitEdgeEvent(original, f1, f2);
	}

	@Override
	public void insertEvent(@NotNull final E halfEdge) {
		pointLocator.insertEvent(getMesh().getVertex(halfEdge));
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
		for(int i=0; i< 10000; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		IPointConstructor<VPoint> pointConstructor =  (x, y) -> new VPoint(x, y);
		long ms = System.currentTimeMillis();

		PMesh<VPoint> mesh = new PMesh<>(pointConstructor);
		ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> bw = ITriangulation.createPTriangulation(
				IPointLocator.Type.DELAUNAY_HIERARCHY,
				points,
				pointConstructor);
		bw.finish();
		System.out.println(System.currentTimeMillis() - ms);
        Set<VLine> edges = bw.getEdges();

		JFrame window = new JFrame();
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.setBounds(0, 0, max, max);
		window.getContentPane().add(new Lines(edges, points, max));
		window.setVisible(true);


        ms = System.currentTimeMillis();
        ITriangulation<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> bw2 = ITriangulation.createATriangulation(
                IPointLocator.Type.DELAUNAY_HIERARCHY,
                points,
                pointConstructor);
        bw2.finish();
        System.out.println(System.currentTimeMillis() - ms);

        Set<VLine> edges2 = bw2.getEdges();
        JFrame window2 = new JFrame();
        window2.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window2.setBounds(0, 0, max, max);
        window2.getContentPane().add(new Lines(edges2, points, max));
        window2.setVisible(true);

		/*ms = System.currentTimeMillis();
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

		UniformTriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> uniformTriangulation = ITriangulation.createUniformTriangulation(
				IPointLocator.Type.DELAUNAY_TREE,
				new VRectangle(0, 0, width, height),
				10.0,
				(x, y) -> new VPoint(x, y));

		uniformTriangulation.generate();
		Set<VLine> edges3 = uniformTriangulation.getEdges();

		JFrame window3 = new JFrame();
		window3.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window3.setBounds(0, 0, max, max);
		window3.getContentPane().add(new Lines(edges3, edges3.stream().flatMap(edge -> edge.streamPoints()).collect(Collectors.toSet()), max));
		window3.setVisible(true);*/

		/*VRectangle bound = new VRectangle(0, 0, width, height);
		ITriangulation triangulation = ITriangulation.createVPTriangulation(bound);
		VPUniformRefinement uniformRefinement = new VPUniformRefinement(
				triangulation,
				bound,
				Arrays.asList(new VRectangle(200, 200, 100, 200)),
				p -> 10.0);

		uniformRefinement.generate();
		Set<VLine> edges4 = triangulation.getEdges();

		JFrame window4 = new JFrame();
		window4.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window4.setBounds(0, 0, max, max);
		window4.getContentPane().add(new Lines(edges4, edges4.stream().flatMap(edge -> edge.streamPoints()).collect(Collectors.toSet()), max));
		window4.setVisible(true);*/
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
