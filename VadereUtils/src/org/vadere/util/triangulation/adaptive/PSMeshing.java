package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.gen.UniformRefinementTriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * @author Benedikt Zoennchen
 */
public class PSMeshing {

	private BiFunction<Double, Double, Double> f = (l, l_0) -> Math.max(l_0 - l, 0.0);


	private static final Logger log = LogManager.getLogger(PSMeshing.class);

	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction edgeLengthFunc;
	private ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> triangulation;
	private Collection<? extends VShape> obstacleShapes;
	private VRectangle bound;
	private double scalingFactor;
	private PriorityQueue<Pair<PFace<MeshPoint>, Double>> heap;

	private double initialEdgeLen = 10.0;
	private double deps;
	private double sumOfqDesiredEdgeLength;
	private double sumOfqLengths;
	private final int retrangulationRate = 10;
	private int retrangulationCounter = 0;

	public PSMeshing(
			final IDistanceFunction distanceFunc,
			final IEdgeLengthFunction edgeLengthFunc,
			final VRectangle bound,
			final Collection<? extends VShape> obstacleShapes) {

		this.bound = bound;
		this.heap = new PriorityQueue<>(new FaceComparator());
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.obstacleShapes = obstacleShapes;
		this.deps = 1.4901e-8 * initialEdgeLen;
		this.triangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, bound, (x, y) -> new MeshPoint(x, y, false));
	}

	/**
	 * Start with a uniform refined triangulation
	 */
	public void initialize() {
		log.info("##### (start) compute a uniform refined triangulation #####");
		UniformRefinementTriangulation uniformRefinementTriangulation = new UniformRefinementTriangulation(triangulation, bound, obstacleShapes, p -> 0.7, distanceFunc);
		uniformRefinementTriangulation.compute();

		//computeScalingFactor();

		for(PFace<MeshPoint> face : getMesh().getFaces()) {
			heap.add(Pair.of(face, faceToQuality(face)));
		}


		log.info("##### (end) compute a uniform refined triangulation #####");
	}

	public void improve() {
		computeScalingFactor();
		int steps = heap.size();

		List<PFace<MeshPoint>> updated = new ArrayList<>(steps);
		retrangulationCounter++;
		for(int i = 0; i < steps / 100; i++) {
			Pair<PFace<MeshPoint>, Double> pair = heap.poll();
			PFace<MeshPoint> face = pair.getKey();
			for(PHalfEdge<MeshPoint> edge : getMesh().getEdgeIt(face)) {
				MeshPoint p1 = getMesh().getPoint(edge);
				MeshPoint p2 = getMesh().getPoint(getMesh().getPrev(edge));

				VLine line = new VLine(p2.toVPoint(), p1.toVPoint());
				double len = line.length();
				double desiredLen = edgeLengthFunc.apply(line.midPoint()) * Parameters.FSCALE * scalingFactor;
				double lenDiff = Math.max(desiredLen - len, 0);

				VPoint forceDirection = p1.toVPoint().subtract(p2.toVPoint()).norm();
				VPoint partlyForce = forceDirection.scalarMultiply((lenDiff / len));

				updatePoint(p1, partlyForce);
				updatePoint(p2, partlyForce.scalarMultiply(-1.0));
			}

			updated.add(face);
			//heap.add(Pair.of(face, faceToQuality(face)));
		}

		if(retrangulationCounter >= retrangulationRate) {
			retriangulate();
			for(PFace<MeshPoint> face : getMesh().getFaces()) {
				heap.add(Pair.of(face, faceToQuality(face)));
			}
			retrangulationCounter = 0;
		}
		else {
			for(PFace<MeshPoint> face : updated){
				heap.add(Pair.of(face, faceToQuality(face)));
			}
		}

		//retriangulate();
	}

	public double step() {
		computeScalingFactor();
		computeForces();
		//updateVertices();
		retriangulate();
		return 10;
	}

	public void computeForces() {
		for(PHalfEdge<MeshPoint> edge : getMesh().getEdgeIt()) {
			MeshPoint p1 = getMesh().getPoint(edge);

			for(PHalfEdge<MeshPoint> neighbour : getMesh().getIncidentEdges(edge)) {
				MeshPoint p2 = getMesh().getPoint(neighbour);

				VLine line = new VLine(p2.toVPoint(), p1.toVPoint());
				double len = line.length();
				double desiredLen = edgeLengthFunc.apply(line.midPoint()) * Parameters.FSCALE * scalingFactor;
				double lenDiff = Math.max(desiredLen - len, 0);

				VPoint forceDirection = p1.toVPoint().subtract(p2.toVPoint()).norm();
				VPoint partlyForce = forceDirection.scalarMultiply((lenDiff / len));

				updatePoint(p1, partlyForce);
				updatePoint(p2, partlyForce.scalarMultiply(-1.0));
				//p1.increaseVelocity(partlyForce);
				//p2.decreaseVelocity(partlyForce);
			}
		}
	}

	public void updateVertices() {

		for(PVertex<MeshPoint> vertex : getMesh().getVertices()) {
			if(!isFixedVertex(vertex)) {
				IPoint velocity = getVelocity(vertex);
				IPoint movement = velocity.scalarMultiply(Parameters.DELTAT * 0.5);

				if(isMovementLegal(vertex, movement)) {
					moveVertex(vertex, movement);
					projectVertex(vertex);
				}
			}

			resetVelocity(vertex);
		}
	}

	private boolean isMovementLegal(final PVertex<MeshPoint> vertex, final IPoint movement) {
		for(PFace<MeshPoint> face : getMesh().getFaceIt(vertex)) {
			if(getMesh().isBoundary(face) || getMesh().toTriangle(face).distance(movement.add(vertex)) < -deps) {
				return true;
			}
		}
		return false;
	}

	/*public void retriangulate() {
		LinkedList<PHalfEdge<MeshPoint>> stack = new LinkedList<>(triangulation.getMesh().getEdges());
		Set<VLine> markedEdges = new HashSet<>();

		while (!stack.isEmpty()) {
			PHalfEdge<MeshPoint> edge = stack.pop();
			markedEdges.remove(getMesh().toLine(edge));

			if(!getMesh().isBoundary(edge) && triangulation.isIllegal(edge)) {
				triangulation.flip(edge);

				for(PHalfEdge<MeshPoint> candidate : getMesh().getEdgeIt(getMesh().getFace(edge))) {

					VLine line = getMesh().toLine(candidate);
					if(!markedEdges.contains(line)) {
						markedEdges.add(line);
						stack.push(candidate);
					}
				}

				for(PHalfEdge<MeshPoint> candidate : getMesh().getEdgeIt(getMesh().getTwinFace(edge))) {
					VLine line = getMesh().toLine(candidate);

					if(!markedEdges.contains(line)) {
						markedEdges.add(line);
						stack.push(candidate);
					}
				}
			}
		}
	}*/

	public void retriangulate() {
		Set<MeshPoint> points = getMesh().getVertices().stream().map(vertex -> getMesh().getPoint(vertex)).collect(Collectors.toSet());

		triangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new MeshPoint(x, y, false));
		removeTrianglesInsideObstacles();
		triangulation.finalize();
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

	private IMesh<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> getMesh() {
		return triangulation.getMesh();
	}

	private void projectVertex(final PVertex<MeshPoint> vertex) {
		MeshPoint position = getMesh().getPoint(vertex);
		double distance = distanceFunc.apply(position);
		if(distance > 0) {
			double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps,0))) - distance) / deps;
			double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0,deps))) - distance) / deps;
			VPoint projection = new VPoint(dGradPX * distance, dGradPY * distance);
			position.subtract(projection);
		}
	}


	public synchronized Collection<VTriangle> getTriangles() {
		return triangulation.streamTriangles().collect(Collectors.toList());
	}

	private void updatePoint(final MeshPoint point, final IPoint force) {

		if(!point.isFixPoint()) {
			IPoint movement = force.scalarMultiply(Parameters.DELTAT);
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
			//triangulation.legalize(triangulation.getMesh().getPrev(current), p);
			current = triangulation.getMesh().getTwin(triangulation.getMesh().getNext(current));
		}

		for(PHalfEdge<MeshPoint> edge : candidates) {
			//triangulation.legalize(triangulation.getMesh().getPrev(current), p);
		}

	}
	private void computeScalingFactor() {
		Set<VLine> lines = triangulation.getMesh().streamEdges()
				.filter(edge -> !triangulation.getMesh().isDestroyed(edge))
				.map(edge -> triangulation.getMesh().toLine(edge)).collect(Collectors.toSet());

		for(VLine line : lines) {
			VPoint midPoint = line.midPoint();
			double desiredEdgeLength = edgeLengthFunc.apply(midPoint);

			sumOfqDesiredEdgeLength += (desiredEdgeLength * desiredEdgeLength);
			sumOfqLengths += (line.length() * line.length());
		}

		recomputeScalingFactor();
	}

	private void recomputeScalingFactor() {
		scalingFactor = Math.sqrt(sumOfqLengths/sumOfqDesiredEdgeLength);
	}

	private void removeTrianglesInsideObstacles() {
		List<PFace<MeshPoint>> faces = triangulation.getMesh().getFaces();
		for(PFace<MeshPoint> face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
				triangulation.removeFace(face, true);
			}
		}
	}

	private Double faceToQuality(final PFace<MeshPoint> face) {

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

	private class FaceComparator implements Comparator<Pair<PFace<MeshPoint>, Double>> {

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

	private class EdgeComperator implements Comparator<PHalfEdge<MeshPoint>> {

		@Override
		public int compare(final PHalfEdge<MeshPoint> o1, final PHalfEdge<MeshPoint> o2) {
			double f1 = triangulation.getMesh().getPoint(o1).getVelocity().distanceToOrigin();
			double f2 = triangulation.getMesh().getPoint(o2).getVelocity().distanceToOrigin();

			if(f1 > f2) {
				return -1;
			}
			else if(f1 < f2) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}
}
