package org.vadere.util.triangulation.improver;

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
import org.vadere.util.triangulation.triangulator.UniformRefinementTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 */
public class PSMeshing implements IMeshImprover<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> {
	private static final Logger log = LogManager.getLogger(PSMeshing.class);
	private boolean illegalMovement = false;
	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction edgeLengthFunc;
	private ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> triangulation;
	private Collection<? extends VShape> obstacleShapes;
	private ArrayList<Pair<MeshPoint, MeshPoint>> edges;
	private VRectangle bound;
	private double scalingFactor;
	private PriorityQueue<Pair<PFace<MeshPoint>, Double>> heap;

	private double initialEdgeLen = 0.4;
	private double deps;
	private double sumOfqDesiredEdgeLength;
	private double sumOfqLengths;

	private boolean initialized = false;
	private boolean runParallel = false;

	private int numberOfRetriangulations = 0;
	private int numberOfIterations = 0;
	private int numberOfIllegalMovementTests = 0;
	private double minDeltaTravelDistance = 0.0;
	private double delta = Parameters.DELTAT;

	private Object gobalAcessSynchronizer = new Object();

	public PSMeshing(
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
		this.edges = new ArrayList<>();
		this.deps = 1.4901e-8 * initialEdgeLen;
		this.triangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, bound, (x, y) -> new MeshPoint(x, y, false));

        /**
         * Start with a uniform refined triangulation
         */
        log.info("##### (start) generate a uniform refined triangulation #####");
        UniformRefinementTriangulator uniformRefinementTriangulation = new UniformRefinementTriangulator(triangulation, bound, obstacleShapes, p -> edgeLengthFunc.apply(p) * initialEdgeLen, distanceFunc);
        uniformRefinementTriangulation.generate();
        //retriangulate();
        log.info("##### (end) generate a uniform refined triangulation #####");
	}

	@Override
    public synchronized void improve() {
        step();
    }

    @Override
    public ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> getTriangulation() {
        return triangulation;
    }

    @Override
    public synchronized Collection<VTriangle> getTriangles() {
        return triangulation.streamTriangles().collect(Collectors.toList());
    }

    public double getQuality() {
        Collection<PFace<MeshPoint>> faces = getMesh().getFaces();
        return faces.stream().map(face -> faceToQuality(face)).reduce((d1, d2) -> d1 + d2).get() / faces.size();
    }

    // TODO: parallize the whole triangulation
    public void retriangulate() {
        removeBoundaryLowQualityTriangles();
        triangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, getMesh().getPoints(), (x, y) -> new MeshPoint(x, y, false));
        removeTrianglesInsideObstacles();
        triangulation.finalize();
    }

	public void execute() {
		double quality = getQuality();
		while (quality < Parameters.qualityMeasurement) {
			improve();
			quality = getQuality();
			log.info("quality = " + quality);
		}
		retriangulate();
	}

	private void step() {
        // TODO: implement removeBoundaryLowQualityTriangles on the GPU!
		removeBoundaryLowQualityTriangles();

		minDeltaTravelDistance = Double.MAX_VALUE;
		illegalMovement = false;

        // there might be some illegal movements
        if(minDeltaTravelDistance < 0.0) {
           // illegalMovement = isMovementIllegal();
            numberOfIllegalMovementTests++;
        }

        //retriangulate();
        if(illegalMovement) {
            //retriangulate();
            //while (flipEdges());

            numberOfRetriangulations++;
        }
        else {
            flipEdges();
        }

        if(minDeltaTravelDistance <= 0) {
//			computeMaxLegalMovements();
        }


        //long ms = System.currentTimeMillis();
		//ms = System.currentTimeMillis() - ms;
		//log.info("ms: " + ms);
		computeScalingFactor();
		//log.info(scalingFactor);
        computeForces();
		//computeDelta();
		updateVertices();

		numberOfIterations++;
		//log.info("#illegalMovementTests: " + numberOfIllegalMovementTests);
		//log.info("#retriangulations: " + numberOfRetriangulations);
		//log.info("#steps: " + numberOfIterations);
		//log.info("#points: " + getMesh().getVertices().size());
	}

    /**
     * computes the edge forces / velocities for all half-edge i.e. for each edge twice!
     */
    private void computeForces() {
	    streamEdges().forEach(e -> computeForces(e));
	}

    /**
     * computes the edge force / velocity for a single half-edge and adds it to its end vertex.
     *
     * @param edge
     */
    private void computeForces(final PHalfEdge<MeshPoint> edge) {
        MeshPoint p1 = getMesh().getPoint(edge);
        MeshPoint p2 = getMesh().getPoint(getMesh().getPrev(edge));

        double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
        double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;
        double lenDiff = Math.max(desiredLen - len, 0);
        p1.increaseVelocity(new VPoint((p1.getX() - p2.getX()) * (lenDiff / len), (p1.getY() - p2.getY()) * (lenDiff / len)));
    }

    /**
     * moves (which may include a back projection) each vertex according to their forces / velocity
     * and resets their forces / velocities.
     */
    private void updateVertices() {
	    streamVertices().forEach(v -> updateVertex(v));
	}

    /**
     * move the vertex (this may project the vertex back) and resets its velocity / forceå.
     *
     * @param vertex
     */
    private void updateVertex(final PVertex<MeshPoint> vertex) {
		if(!isFixedVertex(vertex)) {
			MeshPoint meshPoint = getMesh().getPoint(vertex);
			IPoint velocity = getVelocity(vertex);
			IPoint movement = velocity.scalarMultiply(delta);
			moveVertex(vertex, movement);
			projectBackVertex(vertex);

			//log.info(movement);

			// we might get a bad case
			double delta = meshPoint.getMaxTraveldistance() - meshPoint.getLastPosition().distance(meshPoint.getX(), meshPoint.getY());

			synchronized (gobalAcessSynchronizer) {
				minDeltaTravelDistance = Math.min(minDeltaTravelDistance, delta);
			}

		}

        getMesh().getPoint(vertex).setVelocity(new VPoint(0,0));
	}

    /**
     * projects the vertex back if it is no longer inside the boundary or inside an obstacle.
     *
     * @param vertex the vertex
     */
    private void projectBackVertex(final PVertex<MeshPoint> vertex) {
        MeshPoint position = getMesh().getPoint(vertex);
        double distance = distanceFunc.apply(position);
        if(distance > 0 || getMesh().isAtBoundary(vertex)) {
            double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps, 0))) - distance) / deps;
            double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0, deps))) - distance) / deps;
            VPoint projection = new VPoint(dGradPX * distance, dGradPY * distance);
            position.subtract(projection);
        }
    }

    /**
     * flips all edges which are illegal. Afterwards the triangulation is delaunay.
     *
     * @return true, if any flip was necessary, false otherwise.
     */
    private boolean flipEdges() {
        boolean anyFlip = false;
        // there is still an illegal edge
        //while (streamEdges().anyMatch(e -> triangulation.isIllegal(e))) {
            streamEdges().filter(e -> triangulation.isIllegal(e)).forEach(e -> triangulation.flip(e));
            anyFlip = true;
        //}
        return anyFlip;
    }

    /**
     * Computation of the global scaling factor which is used to
     */
    private void computeScalingFactor() {
        double edgeLengthSum = streamEdges()
                .map(edge -> getMesh().toLine(edge))
                .mapToDouble(line -> line.length())
                .sum();

        double desiredEdgeLenSum = streamEdges()
                .map(edge -> getMesh().toLine(edge))
                .map(line -> line.midPoint())
                .mapToDouble(midPoint -> edgeLengthFunc.apply(midPoint)).sum();
        scalingFactor =  Math.sqrt((edgeLengthSum * edgeLengthSum) / (desiredEdgeLenSum * desiredEdgeLenSum));
    }








    // helper methods
    private Stream<PHalfEdge<MeshPoint>> streamEdges() {
        return runParallel ? getMesh().streamEdgesParallel() : getMesh().streamEdges();
    }

    private Stream<PVertex<MeshPoint>> streamVertices() {
        return runParallel ? getMesh().streamVerticesParallel() : getMesh().streamVertices();
    }

    private void computeDelta() {
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

    private boolean isMovementIllegal() {
        boolean result = false;
        for(PFace<MeshPoint> face : getMesh().getFaces()) {
            if(!getMesh().isBoundary(face) && !getMesh().isDestroyed(face) && !triangulation.isCCW(face)) {
                result = true;

                // is this a triangle at the boundary+
                for(PHalfEdge<MeshPoint> edge : getMesh().getEdgeIt(face)) {
                    if(getMesh().isAtBoundary(edge)) {
                        log.info("illegal face at the boundary.");
                    }
                    else {
                        log.info("illegal face.");
                    }
                }

            }
        }
        return result;
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

	private boolean isFixedVertex(final PVertex<MeshPoint> vertex) {
		return getMesh().getPoint(vertex).isFixPoint();
	}

	private IPoint getVelocity(final PVertex<MeshPoint> vertex) {
		return getMesh().getPoint(vertex).getVelocity();
	}

	private void moveVertex(final PVertex<MeshPoint> vertex, final IPoint dX) {
		getMesh().getPoint(vertex).add(dX);
	}

	@Override
	public IMesh<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> getMesh() {
		return triangulation.getMesh();
	}

	/*public PriorityQueue<PFace<MeshPoint>> getQuailties() {
		PriorityQueue<PFace<MeshPoint>> heap = new PriorityQueue<>(new FaceComparator());
		heap.addAll(getMesh().getFaces());
		return heap;
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

	}*/


	private void removeTrianglesInsideObstacles() {
		List<PFace<MeshPoint>> faces = triangulation.getMesh().getFaces();
		for(PFace<MeshPoint> face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
				triangulation.removeFace(face, true);
			}
		}
	}

	private void removeLowQualityTriangles() {
		List<PFace<MeshPoint>> faces = getMesh().getFaces();
		for(PFace<MeshPoint> face : faces) {
			if(faceToQuality(face) < Parameters.MIN_QUALITY_TRIANGLE) {
				Optional<PHalfEdge<MeshPoint>> optEdge = getMesh().getLinkToBoundary(face);
				if(optEdge.isPresent() && !getMesh().isBoundary(getMesh().getTwin(getMesh().getNext(optEdge.get())))) {
					PHalfEdge<MeshPoint> edge = getMesh().getNext(optEdge.get());
					projectBackVertex(getMesh().getVertex(edge));
					triangulation.removeFace(face, true);
				}
			}
		}
	}

    private void removeBoundaryLowQualityTriangles() {
        streamEdges()
                .filter(e -> getMesh().isBoundary(e))
                .map(e -> getMesh().getTwinFace(e))
                .filter(face -> !getMesh().isDestroyed(face))
                .filter(face -> faceToQuality(face) < Parameters.MIN_QUALITY_TRIANGLE)
                .collect(Collectors.toList()) // we have to collect since we manipulate / remove faces
                .forEach(face -> triangulation.removeFace(face, true));
    }

	public double faceToQuality(final PFace<MeshPoint> face) {

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
