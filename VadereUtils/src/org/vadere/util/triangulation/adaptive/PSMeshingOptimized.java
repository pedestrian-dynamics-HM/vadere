package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 */
public class PSMeshingOptimized {
	private static final Logger log = LogManager.getLogger(PSMeshingOptimized.class);
	private boolean illegalMovement = false;
	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction edgeLengthFunc;
	private ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> triangulation;
	private Collection<? extends VShape> obstacleShapes;
	private VRectangle bound;
	private double scalingFactor;
	private PriorityQueue<Pair<PFace<MeshPoint>, Double>> heap;

	private double initialEdgeLen = 0.4;
	private double deps;
	private double sumOfqDesiredEdgeLength;
	private double sumOfqLengths;

	private boolean initialized = false;

	private int numberOfRetriangulations = 0;
	private int numberOfIterations = 0;
	private int numberOfIllegalMovementTests = 0;
	private double minDeltaTravelDistance = 0.0;
	private double delta = Parameters.DELTAT;

	private Object gobalAcessSynchronizer = new Object();

	public PSMeshingOptimized(
			final IDistanceFunction distanceFunc,
			final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			final VRectangle bound,
			final Collection<? extends VShape> obstacleShapes) {

		this.bound = bound;
		this.heap = new PriorityQueue<>(new FacePairComparator());
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.initialEdgeLen = initialEdgeLen;
		this.obstacleShapes = obstacleShapes;
		this.deps = 1.4901e-8 * initialEdgeLen;
		this.triangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, bound, (x, y) -> new MeshPoint(x, y, false));
	}

	/**
	 * Start with a uniform refined triangulation
	 */
	public void initialize() {
		log.info("##### (start) generate a uniform refined triangulation #####");
		//UniformRefinementTriangulator uniformRefinementTriangulation = new UniformRefinementTriangulator(triangulation, bound, obstacleShapes, p -> edgeLengthFunc.apply(p) * initialEdgeLen, distanceFunc);
		//uniformRefinementTriangulation.generate();
		retriangulate();
		initialized = true;
		log.info("##### (end) generate a uniform refined triangulation #####");
	}

	public ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> getTriangulation() {
		return triangulation;
	}

	public void executeParallel() {
		if(!initialized) {
			initialize();
		}

		double quality = getQuality();
		while (quality < Parameters.qualityMeasurement) {
			stepParallel();
			quality = getQuality();
			log.info("quality = " + quality);
		}

		computeScalingFactorParallel();
		computeForcesParallel();
		updateVerticesParallel();
		retriangulate();
	}

	public void execute() {

		if(!initialized) {
			initialize();
		}

		double quality = getQuality();
		while (quality < Parameters.qualityMeasurement) {
			step();
			quality = getQuality();
			log.info("quality = " + quality);
		}

		computeScalingFactor();
		computeForce();
		computeDelta();
		updateVertices();
		retriangulate();
	}


	public void stepParallel() {
		minDeltaTravelDistance = Double.MAX_VALUE;
		illegalMovement = false;
		computeScalingFactorParallel();
		computeForcesParallel();
		updateVerticesParallel();

		// there might be some illegal movements
		if(minDeltaTravelDistance < 0.0) {
			illegalMovement = isMovementIllegal();
			numberOfIllegalMovementTests++;
		}

		if(illegalMovement) {
			retriangulate();
			numberOfRetriangulations++;
		}
		else {
			flipEdgesParallel();
		}

		if(minDeltaTravelDistance < 0) {
			computeMaxLegalMovementsParallel();
		}

		numberOfIterations++;
		/*log.info("#illegalMovementTests: " + numberOfIllegalMovementTests);
		log.info("#retriangulations: " + numberOfRetriangulations);
		log.info("#steps: " + numberOfIterations);
		log.info("#points: " + getMesh().getVertices().size());*/
	}

	public void step() {
		minDeltaTravelDistance = Double.MAX_VALUE;
		illegalMovement = false;
		//log.info(scalingFactor);
		computeForce();
		computeScalingFactor();
		//computeDelta();
		updateVertices();

		// there might be some illegal movements
		if(minDeltaTravelDistance < 0.0) {
			illegalMovement = isMovementIllegal();
			numberOfIllegalMovementTests++;
		}

		if(illegalMovement) {
			retriangulate();
			numberOfRetriangulations++;
		}
		else {
			flipEdges();
		}

		if(minDeltaTravelDistance < 0) {
			computeMaxLegalMovements();
		}

		numberOfIterations++;
		log.info("#illegalMovementTests: " + numberOfIllegalMovementTests);
		log.info("#retriangulations: " + numberOfRetriangulations);
		log.info("#steps: " + numberOfIterations);
		log.info("#points: " + getMesh().getVertices().size());
	}

	public boolean isMovementIllegal() {
		for(PFace<MeshPoint> face : getMesh().getFaces()) {
			if(!getMesh().isBoundary(face) && !getMesh().isDestroyed(face) && !triangulation.isCCW(face)) {
				return true;
			}
		}
		return false;
	}

	public void computeForcesParallel() {
		getMesh().streamEdgesParallel().forEach(e -> computeForce(e));
	}

	private void computeForce(final PHalfEdge<MeshPoint> edge) {
		MeshPoint p1 = getMesh().getPoint(edge);
		for(PHalfEdge<MeshPoint> neighbour : getMesh().getIncidentEdgesIt(edge)) {
			MeshPoint p2 = getMesh().getPoint(neighbour);

			VLine line = new VLine(p2.toVPoint(), p1.toVPoint());
			double len = line.length();
			double desiredLen = edgeLengthFunc.apply(line.midPoint()) * Parameters.FSCALE * scalingFactor;
			double lenDiff = Math.max(desiredLen - len, 0);

			//maxMovement = Math.max(lenDiff / desiredLen, maxMovement);
			VPoint forceDirection = p1.toVPoint().subtract(p2.toVPoint());
			VPoint partlyForce = forceDirection.scalarMultiply((lenDiff / len));

			//updatePoint(p1, partlyForce);
			//updatePoint(p2, partlyForce.scalarMultiply(-1.0));
			//synchronized (p1) {
				p1.increaseVelocity(partlyForce);
			//}

			//synchronized (p2) {
				p2.decreaseVelocity(partlyForce);
			//}

		}
	}

	// optimized computation
	public void computeForce() {
		for(PHalfEdge<MeshPoint> edge : getMesh().getEdgeIt()) {
			MeshPoint p1 = getMesh().getPoint(edge);
			MeshPoint p2 = getMesh().getPoint(getMesh().getPrev(edge));

			double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
			double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;
			double lenDiff = Math.max(desiredLen - len, 0);
			p1.increaseVelocity(new VPoint((p1.getX() - p2.getX()) * (lenDiff / len), (p1.getY() - p2.getY()) * (lenDiff / len)));


			//maxMovement = Math.max(lenDiff / desiredLen, maxMovement);
			//VPoint forceDirection = p1.toVPoint().subtract(p2.toVPoint());
			//VPoint partlyForce = forceDirection.scalarMultiply((lenDiff / len));

			//updatePoint(p1, partlyForce);
			//updatePoint(p2, partlyForce.scalarMultiply(-1.0));
			//synchronized (p1) {
			//p1.increaseVelocity(partlyForce);
			//}

			//synchronized (p2) {
			//p2.decreaseVelocity(partlyForce);
			//}

		}
	}


	public void computeForceA() {
		for(PHalfEdge<MeshPoint> edge : getMesh().getEdgeIt()) {
			computeForce(edge);
		}
	}

	public void updateVerticesParallel() {
		getMesh().streamVerticesParallel().parallel().forEach(v -> updateVertex(v));
	}

	public void updateVertices() {
		for(PVertex<MeshPoint> vertex : getMesh().getVertices()) {
			updateVertex(vertex);
		}
	}

	public double getQuality() {
		Collection<PFace<MeshPoint>> faces = getMesh().getFaces();
		return faces.stream().map(face -> faceToQuality(face)).reduce((d1, d2) -> d1 + d2).get() / faces.size();
	}

	public void computeDelta() {
		delta = Parameters.DELTAT;
		for(PVertex<MeshPoint> vertex : getMesh().getVertices()) {
			if(!isFixedVertex(vertex)) {
				MeshPoint meshPoint = getMesh().getPoint(vertex);
				IPoint velocity = getVelocity(vertex);

				double desiredLen = edgeLengthFunc.apply(vertex) * Parameters.FSCALE * scalingFactor;
				delta = Math.min(delta, desiredLen * 0.1 / velocity.distanceToOrigin() );
			}
		}
		//log.info("delta: " + delta);
	}

	public void updateVertex(final PVertex<MeshPoint> vertex) {
		if(!isFixedVertex(vertex)) {
			MeshPoint meshPoint = getMesh().getPoint(vertex);
			IPoint velocity = getVelocity(vertex);
			IPoint movement = velocity.scalarMultiply(delta);

			double desiredLen = edgeLengthFunc.apply(vertex) * Parameters.FSCALE * scalingFactor;
			moveVertex(vertex, movement);
			projectBackVertex(vertex);

			//log.info(movement);

			// we might get a bad case
			double delta = meshPoint.getMaxTraveldistance() - meshPoint.getLastPosition().distance(meshPoint.getX(), meshPoint.getY());

			synchronized (gobalAcessSynchronizer) {
				minDeltaTravelDistance = Math.min(minDeltaTravelDistance, delta);
			}

		}

		resetVelocity(vertex);
	}

	private void computeMaxLegalMovementsParallel() {
		getMesh().streamVerticesParallel().forEach(v -> computeMaxLegalMovement(v));
	}

	private void computeMaxLegalMovements() {
		for(PVertex<MeshPoint> vertex : getMesh().getVertices()) {
			computeMaxLegalMovement(vertex);
		}
	}

	private void computeMaxLegalMovement(final PVertex<MeshPoint> vertex) {
		double maxTravelDistance = Double.MAX_VALUE;
		for(PHalfEdge<MeshPoint> edge : triangulation.getRing1It(vertex)) {
			IPoint p1 = getMesh().getVertex(edge);
			IPoint p2 = getMesh().getVertex(getMesh().getPrev(edge));
			maxTravelDistance = Math.min(maxTravelDistance, GeometryUtils.distanceToLineSegment(p1, p2, vertex));
		}
		getMesh().getPoint(vertex).setLastPosition(new VPoint(vertex.getX(), vertex.getY()));
		getMesh().getPoint(vertex).setMaxTraveldistance(maxTravelDistance * 0.5);
	}

	public void flipEdgesParallel() {
		getMesh().streamEdgesParallel().filter(e -> triangulation.isIllegal(e)).forEach(e -> triangulation.flipSync(e));
	}

	public void flipEdges() {

		// Careful, iterate over all half-edges means iterate over each "real" edge twice!
		for(PHalfEdge<MeshPoint> edge : getMesh().getEdgeIt()) {
			if(triangulation.isIllegal(edge)) {
				triangulation.flip(edge);
			}
		}
	}

	// TODO: parallize the whole triangulation
	public void retriangulate() {
		//Set<MeshPoint> points = getMesh().getVertices().stream().map(vertex -> getMesh().getPoint(vertex)).collect(Collectors.toSet());
		removeLowQualityTriangles();
		triangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, getMesh().getPoints(), (x, y) -> new MeshPoint(x, y, false));
		removeTrianglesInsideObstacles();
		triangulation.finish();
	}

	private boolean isFixedVertex(final PVertex<MeshPoint> vertex) {
		return getMesh().getPoint(vertex).isFixPoint();
	}

	private IPoint getVelocity(final PVertex<MeshPoint> vertex) {
		return getMesh().getPoint(vertex).getVelocity();
	}

	private void resetVelocity(final PVertex<MeshPoint> vertex) {
		getMesh().getPoint(vertex).setVelocity(new VPoint(0,0));
	}


	private void moveVertex(final PVertex<MeshPoint> vertex, final IPoint dX) {
		getMesh().getPoint(vertex).add(dX);
	}

	public IMesh<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> getMesh() {
		return triangulation.getMesh();
	}

	private void projectBackVertex(final PVertex<MeshPoint> vertex) {
		MeshPoint position = getMesh().getPoint(vertex);
		double distance = distanceFunc.apply(position);
		if(distance > 0) {
			double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps, 0))) - distance) / deps;
			double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0, deps))) - distance) / deps;
			VPoint projection = new VPoint(dGradPX * distance, dGradPY * distance);
			position.subtract(projection);
		}
	}

	public PriorityQueue<PFace<MeshPoint>> getQuailties() {
		PriorityQueue<PFace<MeshPoint>> heap = new PriorityQueue<>(new FaceComparator());
		heap.addAll(getMesh().getFaces());
		return heap;
	}


	public synchronized Collection<VTriangle> getTriangles() {
		return triangulation.streamTriangles().collect(Collectors.toList());
	}

	private void updatePoint(final MeshPoint point, final IPoint force) {

		if(!point.isFixPoint()) {
			IPoint movement = force.scalarMultiply(delta);
			point.add(movement);

			double distance = distanceFunc.apply(point);
			if(distance > 0) {
				double dGradPX = (distanceFunc.apply(point.toVPoint().add(new VPoint(deps,0))) - distance) / deps;
				double dGradPY = (distanceFunc.apply(point.toVPoint().add(new VPoint(0,deps))) - distance) / deps;

				point.subtract(new VPoint(dGradPX * distance, dGradPY * distance));
			}
		}
	}

	private void adjustMesh(final PVertex<MeshPoint> vertex) {
		PHalfEdge<MeshPoint> halfEdge = triangulation.getMesh().getEdge(vertex);
		PVertex<MeshPoint> p = triangulation.getMesh().getVertex(halfEdge);
		PHalfEdge<MeshPoint> current = halfEdge;
		boolean first = true;

		List<PHalfEdge<MeshPoint>> candidates = new ArrayList<>();
		while (first || current != halfEdge) {
			first = false;
			candidates.add(triangulation.getMesh().getPrev(current));
			//triangulation.legalizeRecursively(triangulation.getMesh().getPrev(current), p);
			current = triangulation.getMesh().getTwin(triangulation.getMesh().getNext(current));
		}

		for(PHalfEdge<MeshPoint> edge : candidates) {
			//triangulation.legalizeRecursively(triangulation.getMesh().getPrev(current), p);
		}

	}

	private void computeScalingFactorParallel() {
		Pair<Double, Double> partialSums = triangulation.getMesh().streamEdgesParallel()
				.map(edge -> triangulation.getMesh().toLine(edge))
				.map(line -> partialSum(line))
				.reduce(Pair.of(0.0, 0.0), (p1, p2) -> Pair.of(p1.getLeft() + p2.getLeft(), p1.getRight() + p2.getRight()));
		scalingFactor =  Math.sqrt(partialSums.getRight() / partialSums.getLeft());

	}

	private Pair<Double, Double> partialSum(final VLine line ) {
		VPoint midPoint = line.midPoint();
		double desiredEdgeLength = edgeLengthFunc.apply(midPoint);
		return Pair.of((desiredEdgeLength * desiredEdgeLength), (line.length() * line.length()));
	}

	/**
	 * Computation of the global scaling factor which is used to
	 */
	private void computeScalingFactor() {
		Pair<Double, Double> partialSums = triangulation.getMesh().streamEdges()
				.map(edge -> triangulation.getMesh().toLine(edge))
				.map(line -> partialSum(line))
				.reduce(Pair.of(0.0, 0.0), (p1, p2) -> Pair.of(p1.getLeft() + p2.getLeft(), p1.getRight() + p2.getRight()));
		scalingFactor =  Math.sqrt(partialSums.getRight() / partialSums.getLeft());
	}


	private void removeTrianglesInsideObstacles() {
		List<PFace<MeshPoint>> faces = triangulation.getMesh().getFaces();
		for(PFace<MeshPoint> face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
				triangulation.removeFaceAtBorder(face, true);
			}
		}
	}

	private void removeLowQualityTriangles() {
		List<PFace<MeshPoint>> faces = getMesh().getFaces();
		for(PFace<MeshPoint> face : faces) {
			if(faceToQuality(face) < Parameters.MIN_TRIANGLE_QUALITY) {
				Optional<PHalfEdge<MeshPoint>> optEdge = getMesh().getLinkToBoundary(face);
				if(optEdge.isPresent() && !getMesh().isBoundary(getMesh().getTwin(getMesh().getNext(optEdge.get())))) {
					PHalfEdge<MeshPoint> edge = getMesh().getNext(optEdge.get());
					projectToBoundary(getMesh().getVertex(edge));
					triangulation.removeFaceAtBorder(face, true);
				}
			}
		}
	}


	private void projectToBoundary(final PVertex<MeshPoint> vertex) {
		MeshPoint position = getMesh().getPoint(vertex);
		double distance = distanceFunc.apply(position);
		if(distance < 0) {
			double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps,0))) - distance) / deps;
			double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0,deps))) - distance) / deps;
			VPoint projection = new VPoint(dGradPX * distance, dGradPY * distance);
			position.subtract(projection);
		}
	}


	private double faceToQuality(final PFace<MeshPoint> face) {

		VLine[] lines = getMesh().toTriangle(face).getLines();
		double a = lines[0].length();
		double b = lines[1].length();
		double c = lines[2].length();
		double part = 0.0;
		if(a != 0.0 && b != 0.0 && c != 0.0) {
			part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
		}
		else {
			throw new IllegalArgumentException(face + " is not a feasible triangle!");
		}
		return part;
	}

	private class FacePairComparator implements Comparator<Pair<PFace<MeshPoint>, Double>> {

		@Override
		public int compare(Pair<PFace<MeshPoint>, Double> o1, Pair<PFace<MeshPoint>, Double> o2) {
			Double q1 = o1.getRight();
			Double q2 = o2.getRight();

			if(q1 < q2) {
				return -1;
			}
			else if(q1 > q2) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}

	private class FaceComparator implements Comparator<PFace<MeshPoint>> {

		@Override
		public int compare(PFace<MeshPoint> o1, PFace<MeshPoint> o2) {
			Double q1 = faceToQuality(o1);
			Double q2 = faceToQuality(o2);

			if(q1 < q2) {
				return -1;
			}
			else if(q1 > q2) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}
}
