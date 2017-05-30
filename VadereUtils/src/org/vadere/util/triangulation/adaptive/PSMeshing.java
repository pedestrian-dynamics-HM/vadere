package org.vadere.util.triangulation.adaptive;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.gen.UniformRefinementTriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 */
public class PSMeshing {

	private static final Logger log = LogManager.getLogger(PSMeshing.class);

	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction edgeLengthFunc;
	private ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> triangulation;
	private Collection<? extends VShape> obstacleShapes;
	private VRectangle bound;
	private double scalingFactor;
	private PriorityQueue<PHalfEdge<MeshPoint>> heap;

	private double deps;

	public PSMeshing(
			final IDistanceFunction distanceFunc,
			final IEdgeLengthFunction edgeLengthFunc,
			final VRectangle bound,
			final Collection<? extends VShape> obstacleShapes) {

		this.bound = bound;
		this.heap = new PriorityQueue<>(new EdgeComperator());
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.obstacleShapes = obstacleShapes;
		this.deps = 1.4901e-8 * 1.0;
		this.triangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, bound, (x, y) -> new MeshPoint(x, y, false));
	}

	/**
	 * Start with a uniform refined triangulation
	 */
	public void initialize() {
		log.info("##### (start) compute a uniform refined triangulation #####");
		UniformRefinementTriangulation uniformRefinementTriangulation = new UniformRefinementTriangulation(triangulation, bound, obstacleShapes, edgeLengthFunc);
		uniformRefinementTriangulation.compute();

		for(PHalfEdge<MeshPoint> edge : triangulation.getMesh().getEdgeIt()) {
			MeshPoint point = triangulation.getMesh().getPoint(edge);
			point.setVelocity(computeDirectedForces(edge));
			heap.add(edge);
		}

		triangulation.getMesh().streamEdges().forEach(halfEdge -> heap.add(halfEdge));

		scalingFactor = computeScalingFactor();
		log.info("##### (end) compute a uniform refined triangulation #####");
	}

	public void improve() {
		PHalfEdge<MeshPoint> halfEdge = heap.poll();
		while (movePoint(halfEdge) < Parameters.TOL);
	}

	public double step() {
		PHalfEdge<MeshPoint> halfEdge = heap.poll();
		return movePoint(halfEdge);
	}

	public Collection<VTriangle> getTriangles() {
		return triangulation.streamTriangles().collect(Collectors.toList());
	}

	private double movePoint(final PHalfEdge<MeshPoint> halfEdge) {
		//recompute!
		VPoint directedForce = computeDirectedForces(halfEdge);
		MeshPoint point = triangulation.getMesh().getPoint(halfEdge);
		IPoint movement = directedForce.scalarMultiply(Parameters.DELTAT);

		double movementLen = point.getVelocity().distanceToOrigin();

		if(!point.isFixPoint()) {
			point.add(movement);

			// recompute
			point.setVelocity(computeDirectedForces(halfEdge));
			heap.remove(halfEdge);
			heap.add(halfEdge);

			double distance = distanceFunc.apply(point);
			if(distance > 0) {
				double dGradPX = (distanceFunc.apply(point.add(new VPoint(deps,0))) - distance) / deps;
				double dGradPY = (distanceFunc.apply(point.add(new VPoint(0,deps))) - distance) / deps;

				point.subtract(new VPoint(dGradPX * distance, dGradPY * distance));
			}
		}

		// update neighbours
		for(PHalfEdge<MeshPoint> neighbour : triangulation.getMesh().getIncidentEdgesIt(halfEdge)) {

			MeshPoint nPoint = triangulation.getMesh().getPoint(neighbour);
			nPoint.setVelocity(computeDirectedForces(neighbour));

			heap.remove(neighbour);
			heap.add(neighbour);
		}

		// local changes. TODO: does a vertex may leave a face?
		adjustMesh(halfEdge);

		return movementLen;
	}

	private void adjustMesh(final PHalfEdge<MeshPoint> halfEdge) {
		PVertex<MeshPoint> p = triangulation.getMesh().getVertex(halfEdge);
		for(PHalfEdge<MeshPoint> neighbour : triangulation.getMesh().getIncidentEdgesIt(halfEdge)) {
			triangulation.legalize(neighbour, p);
		}
	}

	/*private VPoint computeDirectedForce(final PHalfEdge<MeshPoint> edge) {
		VLine line = edge.toLine();
		double len = line.length();
		double desiredLen = edgeLengthFunc.apply(line.midPoint()) * Parameters.FSCALE * scalingFactor;
		double lenDiff = Math.max(desiredLen - len, 0);

		return new VPoint((lenDiff / len) * (line.getX1() - line.getX2()), (lenDiff / len) * (line.getY1() - line.getY2()));
	}*/

	private VPoint computeDirectedForces(final PHalfEdge<MeshPoint> edge) {
		MeshPoint p1 = triangulation.getMesh().getPoint(edge);
		VPoint directedForce = new VPoint();

		for(PHalfEdge<MeshPoint> neighbour : triangulation.getMesh().getIncidentEdgesIt(edge)) {
			MeshPoint p2 = triangulation.getMesh().getPoint(neighbour);

			VLine line = new VLine(p1.toVPoint(), p2.toVPoint());
			double len = line.length();
			double desiredLen = edgeLengthFunc.apply(line.midPoint()) * Parameters.FSCALE * scalingFactor;
			double lenDiff = Math.max(desiredLen - len, 0);

			directedForce = directedForce.add(new VPoint((lenDiff / len) * (line.getX1() - line.getX2()), (lenDiff / len) * (line.getY1() - line.getY2())));
		}

		return directedForce;
	}

	private double computeScalingFactor() {
		double sumOfqDesiredEdgeLength = 0;
		double sumOfqLengths = 0;

		for(PHalfEdge<MeshPoint> edge : triangulation.getMesh().getEdgeIt()) {
			VLine line = edge.toLine();

			VPoint midPoint = line.midPoint();
			double desiredEdgeLength = edgeLengthFunc.apply(midPoint);

			// * 0.5 since we get each edge twice
			sumOfqDesiredEdgeLength += (desiredEdgeLength * desiredEdgeLength) * 0.5;
			sumOfqLengths += (line.length() * line.length());
		}

		return Math.sqrt(sumOfqLengths/sumOfqDesiredEdgeLength);
	}

	/*private class MovingEdge {
		public PHalfEdge<MeshPoint> halfEdge;
		public VPoint velocity
	}*/

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
