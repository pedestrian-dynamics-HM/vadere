package org.vadere.util.triangulation.improver;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.triangulation.ITriangulationSupplier;
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
import org.vadere.util.triangulation.triangulator.UniformRefinementTriangulator;
import org.vadere.util.triangulation.triangulator.UniformRefinementTriangulatorCFS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 */
public class PSMeshing<P extends MeshPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements IMeshImprover<P, V, E, F> {

	private boolean illegalMovement = false;
	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction edgeLengthFunc;
	private ITriangulation<P, V, E, F> triangulation;
	private VRectangle bound;
	private double scalingFactor;
	private double deps;

	private boolean runParallel = true;
	private double minDeltaTravelDistance = 0.0;
	private double delta = Parameters.DELTAT;

    private Object gobalAcessSynchronizer = new Object();

	// only for logging
    private static final Logger log = LogManager.getLogger(PSMeshing.class);
	private int numberOfIllegalMovementTests;
	private int numberOfRetriangulations;
	private int numberOfIterations;

	public PSMeshing(
            final IDistanceFunction distanceFunc,
            final IEdgeLengthFunction edgeLengthFunc,
            final double initialEdgeLen,
            final VRectangle bound,
            final Collection<? extends VShape> obstacleShapes,
            final ITriangulationSupplier<P, V, E, F> triangulationSupplier) {

		this.bound = bound;
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.deps = 1.4901e-8 * initialEdgeLen;

        log.info("##### (start) generate a triangulation #####");
        UniformRefinementTriangulatorCFS<P, V, E, F> uniformRefinementTriangulation = new UniformRefinementTriangulatorCFS(
                triangulationSupplier,
                bound,
                obstacleShapes,
                p -> edgeLengthFunc.apply(p) * initialEdgeLen,
                distanceFunc);
        triangulation = uniformRefinementTriangulation.generate();
        log.info("##### (end) generate a triangulation #####");
	}

	@Override
    public synchronized void improve() {
        step();
    }

    @Override
    public ITriangulation<P, V, E, F> getTriangulation() {
        return triangulation;
    }

    @Override
    public synchronized Collection<VTriangle> getTriangles() {
        return triangulation.streamTriangles().collect(Collectors.toList());
    }

    public double getQuality() {
        Collection<F> faces = getMesh().getFaces();
        return faces.stream().map(face -> faceToQuality(face)).reduce((d1, d2) -> d1 + d2).get() / faces.size();
    }

    // TODO: parallize the whole triangulation
    public void retriangulate() {
        removeBoundaryLowQualityTriangles();
        triangulation.recompute();
        removeTrianglesInsideObstacles();
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
		//removeBoundaryLowQualityTriangles();

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
    private void computeForces(final E edge) {
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
    private void updateVertex(final V vertex) {
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
    private void projectBackVertex(final V vertex) {
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
        log.info("scale factor = " + scalingFactor);
    }








    // helper methods
    private Stream<E> streamEdges() {
        return runParallel ? getMesh().streamEdgesParallel() : getMesh().streamEdges();
    }

    private Stream<V> streamVertices() {
        return runParallel ? getMesh().streamVerticesParallel() : getMesh().streamVertices();
    }

    private void computeDelta() {
        delta = Parameters.DELTAT;
        for(V vertex : getMesh().getVertices()) {
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
        for(F face : getMesh().getFaces()) {
            if(!getMesh().isBoundary(face) && !getMesh().isDestroyed(face) && !triangulation.isCCW(face)) {
                result = true;

                // is this a triangle at the boundary+
                for(E edge : getMesh().getEdgeIt(face)) {
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


	private boolean isFixedVertex(final V vertex) {
		return getMesh().getPoint(vertex).isFixPoint();
	}

	private IPoint getVelocity(final V vertex) {
		return getMesh().getPoint(vertex).getVelocity();
	}

	private void moveVertex(final V vertex, final IPoint dX) {
		getMesh().getPoint(vertex).add(dX);
	}

	@Override
	public IMesh<P, V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	/*public PriorityQueue<PFace<MeshPoint>> getQuailties() {
		PriorityQueue<PFace<MeshPoint>> heap = new PriorityQueue<>(new FaceComparator());
		heap.addAll(getMesh().getTriangles());
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
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
				triangulation.removeFace(face, true);
			}
		}
	}

	private void removeLowQualityTriangles() {
		List<F> faces = getMesh().getFaces();
		for(F face : faces) {
			if(faceToQuality(face) < Parameters.MIN_QUALITY_TRIANGLE) {
				Optional<E> optEdge = getMesh().getLinkToBoundary(face);
				if(optEdge.isPresent() && !getMesh().isBoundary(getMesh().getTwin(getMesh().getNext(optEdge.get())))) {
					E edge = getMesh().getNext(optEdge.get());
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

	public double faceToQuality(final F face) {

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
}
