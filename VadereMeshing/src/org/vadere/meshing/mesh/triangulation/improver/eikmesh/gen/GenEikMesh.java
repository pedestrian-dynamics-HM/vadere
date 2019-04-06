package org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.examples.MeshExamples;
import org.vadere.meshing.mesh.IllegalMeshException;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.improver.IMeshImprover;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Parameters;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRivaraRefinement;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformRefinementTriangulatorSFC;
import org.vadere.util.data.cellgrid.IPotentialPoint;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.DistanceFunction;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.util.math.InterpolationUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 */
public class GenEikMesh<P extends EikMeshPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements IMeshImprover<P, CE, CF, V, E, F>, ITriangulator<P, CE, CF, V, E, F> {

	private IRefiner<P, CE, CF, V, E, F> refiner;
	private IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation;

	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction edgeLengthFunc;
	private VRectangle bound;
	private Collection<VPoint> fixPoints;
	private double scalingFactor;
	private double deps;
	private static final int MAX_STEPS = 2000;
	private int nSteps;
	private double initialEdgeLen;

	private boolean initialized = false;
	private boolean runParallel = false;
	private boolean profiling = false;
	private double minDeltaTravelDistance = 0.0;
	private double delta = Parameters.DELTAT;
	private Collection<? extends VShape> shapes;
	private Map<V, VPoint> fixPointRelation;
	private boolean nonEmptyBaseMode;

	// different options
	private boolean allowEdgeSplits = false;
	private boolean allowVertexCollapse = false;
	private boolean useVirtualEdges = true;
	private boolean useFixPoints = true;

	// only for logging
    private static final Logger log = Logger.getLogger(GenEikMesh.class);

	/**
	 * Constructor to use EikMesh on an existing {@link org.vadere.meshing.mesh.inter.ITriangulation}, that is
	 * EikMesh uses this triangulation as a bases. It will refine the triangulation by using a longest edge
	 * split strategy {@link GenRivaraRefinement} to generate desired edge length determined by
	 * len(p) = <tt>initialEdgeLen</tt> * <tt>edgeLengthFunc(p)</tt>.
	 *
	 * Assumption:
	 * <ol>
	 *     <ul><tt>edgeLengthFunc</tt> should be something like <tt>edgeLengthFunc</tt>(p) = 1 + f(p) and should be >= 1 everywhere!</ul>
	 *     <ul><tt>triangulation</tt> should be a valid triangulation with angles >= 20 degree!</ul>
	 * </ol>
	 *
	 *
	 * @param edgeLengthFunc    the relative desired edge length function
	 * @param triangulation     a valid triangulation
	 */
	public GenEikMesh(
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			@NotNull final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation) {
		this.shapes = new ArrayList<>();
		this.bound = null;
		this.edgeLengthFunc = edgeLengthFunc;
		this.nSteps = 0;
		this.fixPointRelation = new HashMap<>();
		this.triangulation = triangulation;
		this.refiner = null;
		this.distanceFunc = null;
		this.initialEdgeLen = triangulation.getMesh().streamEdges().map(e -> getMesh().toLine(e).length()).min(Double::compareTo).orElse(1.0);
		this.deps = 0.0001 * initialEdgeLen;
		this.nonEmptyBaseMode = true;
		this.fixPoints = Collections.EMPTY_LIST;
	}

	/**
	 * Constructor to use EikMesh on an existing {@link org.vadere.meshing.mesh.inter.ITriangulation}, that is
	 * EikMesh uses this triangulation as a bases. It will refine the triangulation by using a longest edge
	 * split strategy {@link GenRivaraRefinement} to generate desired edge length determined by
	 * len(p) = <tt>initialEdgeLen</tt> * <tt>edgeLengthFunc(p)</tt>.
	 *
	 * Assumption:
	 * <ol>
	 *     <ul><tt>edgeLengthFunc</tt> should be something like <tt>edgeLengthFunc</tt>(p) = 1 + f(p) and should be >= 1 everywhere!</ul>
	 *     <ul><tt>triangulation</tt> should be a valid triangulation with angles >= 20 degree!</ul>
	 * </ol>
	 *
	 *
	 * @param distanceFunc      the signed distance function
	 * @param edgeLengthFunc    the relative desired edge length function
	 * @param initialEdgeLen    the initial edge length i.e. approximately the minimum edge length
	 * @param triangulation     a valid triangulation
	 */
	public GenEikMesh(
			@NotNull final IDistanceFunction distanceFunc,
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			@NotNull final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation) {
		this.shapes = new ArrayList<>();
		this.bound = null;
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.initialEdgeLen =initialEdgeLen;
		this.deps = 0.0001 * initialEdgeLen;
		this.nSteps = 0;
		this.fixPointRelation = new HashMap<>();
		this.refiner = new GenRivaraRefinement<>(triangulation, p -> initialEdgeLen * edgeLengthFunc.apply(p));
		this.nonEmptyBaseMode = true;
		this.fixPoints = Collections.EMPTY_LIST;
	}

	public GenEikMesh(
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier) {
		this.distanceFunc = GenEikMesh.createDistanceFunction(bound, constrains);
		GenRuppertsTriangulator<P, CE, CF, V, E, F> ruppertsTriangulator = new GenRuppertsTriangulator(meshSupplier, bound, constrains, Collections.EMPTY_SET, 20);
		IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation = ruppertsTriangulator.generate();
		this.shapes = new ArrayList<>();
		this.bound = null;
		this.edgeLengthFunc = edgeLengthFunc;
		this.initialEdgeLen =initialEdgeLen;
		this.deps = 0.0001 * initialEdgeLen;
		this.nSteps = 0;
		this.fixPointRelation = new HashMap<>();
		this.refiner = new GenRivaraRefinement<>(triangulation, p -> initialEdgeLen * edgeLengthFunc.apply(p));
		this.nonEmptyBaseMode = true;
		this.fixPoints = Collections.EMPTY_LIST;
	}

	/**
	 * Constructor to use EikMesh to construct the whole new triangulation based on a given geometry
	 * defined by a {@link IDistanceFunction} and additionally (optional) by some {@link VShape}s, where
	 * each of the elements in <tt>shapes</tt> is part of the (outside) boundary area.
	 *
	 * @param distanceFunc      the distance function defining the geometry
	 * @param edgeLengthFunc    the relative desired edge lenght function
	 * @param initialEdgeLen    the initial edge length i.e. approximately the minimum edge length
	 * @param bound             the bound which has to contain the whole geometry
	 * @param shapes            a collection of shapes used to generate fix points to improve the result on sharp corners
	 * @param meshSupplier      a mesh supplier which is used to generate the mesh which will be filled
	 */
	public GenEikMesh(
            @NotNull final IDistanceFunction distanceFunc,
            @NotNull final IEdgeLengthFunction edgeLengthFunc,
            @NotNull final Collection<VPoint> fixPoints,
            final double initialEdgeLen,
            @NotNull final VRectangle bound,
            @NotNull final Collection<? extends VShape> shapes,
            @NotNull final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier) {
		this.shapes = shapes;
		this.bound = bound;
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.initialEdgeLen =initialEdgeLen;
		this.deps = 0.0001 * initialEdgeLen;
		this.nSteps = 0;
		this.fixPointRelation = new HashMap<>();
		this.nonEmptyBaseMode = false;
		this.fixPoints = fixPoints;
		this.refiner = new GenUniformRefinementTriangulatorSFC(
				meshSupplier,
				bound,
				shapes,
				edgeLengthFunc,
				initialEdgeLen,
				distanceFunc);
	}

	public GenEikMesh(
			@NotNull final IDistanceFunction distanceFunc,
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			@NotNull final VRectangle bound,
			@NotNull final Collection<? extends VShape> shapes,
			@NotNull final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier) {
		this(distanceFunc, edgeLengthFunc, Collections.EMPTY_LIST, initialEdgeLen, bound, shapes, meshSupplier);
	}

	public GenEikMesh(
			@NotNull final IDistanceFunction distanceFunc,
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			@NotNull final VRectangle bound,
			@NotNull final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier) {
		this(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, Collections.EMPTY_LIST, meshSupplier);
	}

	public GenEikMesh(@NotNull final VPolygon boundary,
	                  final double initialEdgeLen,
	                  @NotNull final Collection<? extends VShape> shapes,
	                  @NotNull final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier){
		this(new DistanceFunction(boundary, shapes), p -> 1.0, initialEdgeLen, GeometryUtils.boundRelative(boundary.getPoints()), shapes, meshSupplier);
	}

	public void step() {
		improve();
	}

	public void initialize() {
		while (refiner != null && !refiner.isFinished()) {
			refiner.refine();
		}
		if(nonEmptyBaseMode) {
			getMesh().streamBoundaryEdges().map(e -> getMesh().getPoint(e)).forEach(p -> p.setFixPoint(true));
		}
		computeAnchorPointRelation();
		initialized = true;
	}

	public boolean initializationFinished() {
		return initialized;
	}

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> generate() {
		return generate(true);
	}

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> generate(boolean finalize) {
		if(!initializationFinished()) {
			initialize();
		}

		double quality = getQuality();
		//log.info("quality: " + quality);
		while (quality < Parameters.qualityMeasurement && nSteps < MAX_STEPS) {
			improve();
			quality = getQuality();
			//log.info("quality: " + quality);
		}

		if(finalize) {
			finish();
			//removeTrianglesInsideHoles();
			//removeTrianglesOutsideBBox();
			getMesh().garbageCollection();
		}

		return getTriangulation();
	}

	public boolean isFinished() {
		return /*getQuality() >= Parameters.qualityMeasurement || */ nSteps >= MAX_STEPS;
	}


	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return getTriangulation().getMesh();
	}

	@Override
    public void improve() {
		synchronized (getMesh()) {

			if(!initializationFinished()) {
				initialize();
			}
			else {
				//updateFaces();
				if(!nonEmptyBaseMode) {
					removeFacesAtBoundary();
					if(distanceFunc != null) {
						getTriangulation().smoothBoundary(distanceFunc);
					}

				}

				//clearPoorTriangles();
				if(getTriangulation().isValid()) {
					flipEdges();
				}
				else if(distanceFunc != null) {
					retriangulate();
				}
				else {
					// this should never happen
					throw new IllegalArgumentException("error!");
				}

				scalingFactor = computeEdgeScalingFactor(edgeLengthFunc);
				computeVertexForces();
				//computeBoundaryForces();

				updateEdges();
				updateVertices();

				log.info("quality = " + getQuality());
				nSteps++;
			}
		}
    }

    public void finish() {
		if(distanceFunc != null){
			try {
				removeFacesOutside(distanceFunc);
			} catch (IllegalMeshException e) {
				e.printStackTrace();
			}
		}
    }

    private void clearPoorTriangles() {
		for(V vertex : getMesh().getVertices()) {
			if(!getMesh().isAtBoundary(vertex)) {
				for(E edge : getMesh().getEdgeIt(vertex)) {
					E prev = getMesh().getPrev(edge);
					VLine line = getMesh().toLine(prev);
					double distance = GeometryUtils.distanceToLineSegment(line.getX1(), line.getY1(), line.getX2(), line.getY2(), vertex.getX(), vertex.getY());
					if(distance < initialEdgeLen * 0.1) {
						getTriangulation().remove(vertex);
						break;
					}
				}
			}
		}
    }

    private void retriangulate() {
	    log.info("EikMesh re-triangulates in step " + nSteps);
	    getTriangulation().recompute();
	    shrinkBorder();
	    createHoles();
	    //removeTrianglesOutsideBBox();
	    //removeTrianglesInsideObstacles();
	    try {
		    removeFacesOutside(distanceFunc);
	    } catch (IllegalMeshException e) {
		    log.error("error!");
	    }
    }

    @Override
    public IIncrementalTriangulation<P, CE, CF, V, E, F> getTriangulation() {
		return refiner != null ? refiner.generate() : triangulation;
    }

    @Override
    public synchronized Collection<VTriangle> getTriangles() {
        return getTriangulation().streamTriangles().collect(Collectors.toList());
    }

    /**
     * computes the edge forces / velocities for all half-edge i.e. for each edge twice!
     */
    private void computeForces() {
	    streamEdges().forEach(e -> computeForces(e));
	}

	/**
	 * computes the edge forces / velocities for all half-edge i.e. for each edge twice!
	 */
	private void computeBoundaryForces() {
		getMesh().streamBoundaryEdges().forEach(e -> computeBoundaryForces(e));
	}


	private void computeVertexForces() {
    	streamVertices().forEach(v -> computeForce(v));
	}

	/**
	 * Computes and sets the overall force acting on a vertex. If the vertex is an anchor vertex
	 * the force will be different: It will act towards the anchor point.
	 *
	 * @param vertex the vertex of interest
	 */
	private void computeForce(final V vertex) {
		EikMeshPoint p1 = getMesh().getPoint(vertex);

		if(useFixPoints && fixPointRelation.containsKey(vertex)) {
			VPoint p2 = fixPointRelation.get(vertex);
			if(p2.distanceSq(vertex.getX(), vertex.getY()) <= deps) {
				getMesh().getPoint(vertex).set(p2.getX(), p2.getY());
			} else {
				VPoint force = new VPoint((p2.getX() - p1.getX()), (p2.getY() - p1.getY())).scalarMultiply(1.0);
				p1.increaseVelocity(force);
				p1.increaseAbsoluteForce(force.distanceToOrigin());
			}
		}
		else {
			for(E edge : getMesh().getEdgeIt(vertex)) {
				V v2 = getMesh().getVertex(getMesh().getTwin(edge));
				EikMeshPoint p2 = getMesh().getPoint(v2);

				double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
				double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;

				double lenDiff = Math.max(desiredLen - len, 0);
				//double lenDiff = desiredLen - len;

				if(lenDiff < 0 /*&& lenDiff > -desiredLen*/) {
					lenDiff *= 0.1;
				}

				VPoint force = new VPoint((p1.getX() - p2.getX()) * (lenDiff / len), (p1.getY() - p2.getY()) * (lenDiff / len));
				p1.increaseVelocity(force);
				p1.increaseAbsoluteForce(force.distanceToOrigin());

				if(useVirtualEdges) {
					E prev = getMesh().getPrev(edge);
					if(getMesh().isAtBoundary(prev)) {
						VLine line = getMesh().toLine(prev);
						VPoint midpoint = line.midPoint();
						double s = line.length();
						VPoint dir = midpoint.subtract(p1).scalarMultiply(2);
						VPoint p3 = dir.add(p1);
						len = Math.sqrt((p1.getX() - p3.getX()) * (p1.getX() - p3.getX()) + (p1.getY() - p3.getY()) * (p1.getY() - p3.getY()));

						VPoint p = p3;
						if(allowEdgeSplits) {
							desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p.getX()) * 0.5, (p1.getY() + p.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;
						}
						else {
							desiredLen = Math.sqrt(3) * s;
							//desiredLen = Math.sqrt(0.75 * s * s);
						}


						lenDiff = Math.max(desiredLen - len, 0);
						//double lenDiff = desiredLen - len;

						if(lenDiff < 0 /*&& lenDiff > -desiredLen*/) {
							lenDiff *= 0.1;
						}

						force = new VPoint((p1.getX() - p3.getX()) * (lenDiff / len), (p1.getY() - p3.getY()) * (lenDiff / len));
						p1.increaseVelocity(force);
						p1.increaseAbsoluteForce(force.distanceToOrigin());
					}
				}

			}
		}
	}

    /**
     * Computes the edge force / velocity for a single half-edge and adds it to its end vertex.
     *
     * @param edge
     */
    private void computeForces(final E edge) {
    	/*
    	 * like DistMesh
    	 */
        EikMeshPoint p1 = getMesh().getPoint(edge);
        EikMeshPoint p2 = getMesh().getPoint(getMesh().getPrev(edge));

        double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
        VPoint midPoint = new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5);
        double desiredLen = edgeLengthFunc.apply(midPoint) * Parameters.FSCALE * scalingFactor;

		double lenDiff = Math.max(desiredLen - len, 0);
        p1.increaseVelocity(new VPoint((p1.getX() - p2.getX()) * (lenDiff / len), (p1.getY() - p2.getY()) * (lenDiff / len)));


    }

	private void computeBoundaryForces(final E edge) {
		/*
		 * EikMesh improvements
		 */
		if(getMesh().isBoundary(edge)) {
			E twin = getMesh().getTwin(edge);
			E next = getMesh().getNext(twin);
			E prev = getMesh().getPrev(twin);

			if((getMesh().toLine(next).length() + getMesh().toLine(prev).length()) * 1.1 < getMesh().toLine(edge).length()) {
				// get the opposite point
				P p1 = getMesh().getPoint(edge);
				P p2 = getMesh().getPoint(getMesh().getPrev(edge));
				P p3 = getMesh().getPoint(getMesh().getNext(getMesh().getTwin(edge)));

				VPoint midPoint = new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5);
				VPoint midLeft = new VPoint((p1.getX() + p3.getX()) * 0.5, (p1.getY() + p3.getY()) * 0.5);
				VPoint midRight = new VPoint((p2.getX() + p3.getX()) * 0.5, (p2.getY() + p3.getY()) * 0.5);
				VPoint q = new VPoint((midPoint.getX() + p3.getX()) * 0.5, (midPoint.getY() + p3.getY()) * 0.5);

				double desiredLen = edgeLengthFunc.apply(q) * Parameters.FSCALE * scalingFactor;
				double len = new VLine(midPoint, getMesh().toPoint(p3)).length();

				double cDes = edgeLengthFunc.apply(midLeft);
				double aDes = edgeLengthFunc.apply(new VLine(midLeft, getMesh().toPoint(edge)).midPoint());
				double bDes = Math.sqrt(cDes*cDes - aDes*aDes) * Parameters.FSCALE * scalingFactor;

				double c = getMesh().toLine(getMesh().getPrev(twin)).length();
				double a = getMesh().toLine(edge).length() * 0.5;
				double b = Math.sqrt(c*c - a*a);


				double lenDiff = desiredLen - len;

				if(lenDiff < 0 /*&& lenDiff > -desiredLen*/) {
					lenDiff *= 0.1;
				}

				VPoint force = new VPoint((midLeft.getX() - p3.getX()) * (lenDiff / len), (midLeft.getY() - p3.getY()) * (lenDiff / len));
				p3.increaseVelocity(force);
				p3.increaseAbsoluteForce(force.distanceToOrigin());
			}
		}
	}

	private void computeForcesBossan(final E edge) {
		EikMeshPoint p1 = getMesh().getPoint(edge);
		EikMeshPoint p2 = getMesh().getPoint(getMesh().getPrev(edge));

		double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
		double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;

		double lenDiff = Math.max(desiredLen - len, 0);
		p1.increaseVelocity(new VPoint((p1.getX() - p2.getX()) * (lenDiff / (len / desiredLen)), (p1.getY() - p2.getY()) * (lenDiff / (len / desiredLen))));
	}
    /**
     * Moves (which may include a back projection) each vertex according to their forces / velocity
     * and resets their forces / velocities. A vertex might be broken (removed by edge collapse)
     * if the forces acting on an boundary vertex are to strong.
     */
    private void updateVertices() {
	    streamVertices().forEach(v -> updateVertex(v));
	}

	/**
	 * Updates a vertex which is not a fix point, that is, the computed force is applied and the vertex move
	 * according to this force. Additionally, the vertex might get back projected if it is outside or it might
	 * break if the forces / the pressure are too large.
	 *
	 * @param vertex the vertex
	 */
	private void updateVertex(final V vertex) {
	    // modify point placement only if it is not a fix point
	    P point = getMesh().getPoint(vertex);
    	if(!isFixedVertex(vertex)) {
		    /*
		     * (1) break / remove the vertex if the forces are to large / there is to much pressure
		     */
		    if(canBreak(vertex) && isBreaking(vertex)) {
			    // TODO: if the algorithm runs in parallel this might lead to unexpected results! synchronized required!
			    getTriangulation().collapse3DVertex(vertex, true);
		    }
		    /*
		     * (2) otherwise displace the vertex
		     */
		    else {
			    VPoint oldPosition = new VPoint(vertex.getX(), vertex.getY());

			    // (2.1) p_{k+1} = p_k + dt * F(p_k)
			    applyForce(vertex);

			    // (2.2) back projtion
			    if(distanceFunc != null) {
				    VPoint projection = computeProjection(vertex);
				    point.set(projection.getX(), projection.getY());
			    }
		    }
		}
	    point.setVelocity(new VPoint(0,0));
	    point.setAbsoluteForce(0);
	}

	/**
	 * unused.
	 */
	private void updateFaces() {
		if(nonEmptyBaseMode) {
			getMesh().getFaces().stream().forEach(f -> updateFace(f));
		}
	}

	private void updateFace(@NotNull F face) {
		if(canBreak(face) && isBreaking(face)) {
			VPoint circumcenter = getMesh().toTriangle(face).getCircumcenter();
			getTriangulation().splitTriangle(face, getMesh().createPoint(circumcenter.getX(), circumcenter.getY()), false);
		}
	}

	/**
	 * Updates all boundary edges. Some of those edges might get split.
	 */
	private void updateEdges() {
		getMesh().getBoundaryEdges().forEach(e -> updateEdge(e));
	}

	/**
	 * Splits an edge if necessary.
	 *
	 * @param edge the edge
	 */
	private void updateEdge(@NotNull final E edge) {
		if(canBreak(edge) && isBreaking(edge)) {
			getTriangulation().splitEdge(edge, true, p -> p.setFixPoint(nonEmptyBaseMode));
		}
	}

	/**
	 * unused
	 * @param face
	 * @return
	 */
	private boolean canBreak(@NotNull final F face) {
		return !getMesh().isBoundary(face);
	}

	/**
	 * unused
	 * @param face
	 * @return
	 */
	private boolean isBreaking(@NotNull final F face) {
		if(faceToQuality(face) > 0.95) {
			E edge = getMesh().getEdge(face);
			if(initialEdgeLen * edgeLengthFunc.apply(getMesh().toLine(edge).midPoint()) * 2.1 <= getMesh().toLine(edge).length()) {
				VPoint circumcenter = getMesh().toTriangle(face).getCircumcenter();
				return getMesh().toTriangle(face).contains(circumcenter);
			}
		}
		return false;
	}

	/**
	 * Returns true if the edge can be split, that is the edge can be replaced
	 * by two new edges by splitting the edge and its face into two. The edge
	 * has to be at the boundary.
	 *
	 * @param edge the edge
	 * @return true if the edge can be collapsed / break.
	 */
	private boolean canBreak(@NotNull final E edge) {
		return allowEdgeSplits;
	}

	/**
	 * Returns true if the edge should be split. That is the case if the edge is long with
	 * respect to the other two edges of the face, i.e. it has to be the longest edge and the
	 * quality of the face is low. The analogy is that the edge breaks because it can not
	 * become any longer.
	 *
	 * @param edge the edge which is tested
	 * @return true if the edge breaks under the pressure of the forces, otherwise false.
	 */
	private boolean isBreaking(@NotNull final E edge) {
		return getMesh().isLongestEdge(edge) && faceToQuality(getMesh().getTwinFace(edge)) < Parameters.MIN_SPLIT_TRIANGLE_QUALITY;
	}

	/*private boolean isDoubleLongEdge(@NotNull final E edge) {

		if(!getMesh().isAtBoundary(edge)) {
			VLine line = getMesh().toLine(edge);
			double factor = 1.5;
			VLine line1 = getMesh().toLine(getMesh().getNext(edge));
			VLine line2 = getMesh().toLine(getMesh().getPrev(edge));

			return getMesh().isAtBoundary(getMesh().getNext(edge)) && line1.length() * factor <= line.length()
					|| getMesh().isAtBoundary(getMesh().getPrev(edge)) && line2.length() * factor <= line.length();
		}
		return false;
	}*/

	/*private boolean isDoubleLongEdge(@NotNull final E edge) {
		VLine line = getMesh().toLine(edge);
		double factor = 2.5;
		if(getMesh().isAtBoundary(edge)) {
			VLine line1 = getMesh().toLine(getMesh().getNext(edge));
			VLine line2 = getMesh().toLine(getMesh().getPrev(edge));
			return line.length() >= line1.length() * factor || line.length() >= line2.length() * factor;
		}
		else {
			VLine line1 = getMesh().toLine(getMesh().getNext(getMesh().getTwin(edge)));
			VLine line2 = getMesh().toLine(getMesh().getPrev(getMesh().getTwin(edge)));
			VLine line3 = getMesh().toLine(getMesh().getPrev(edge));
			VLine line4 = getMesh().toLine(getMesh().getPrev(edge));
			return line.length() >= line1.length() * factor || line.length() >= line2.length() * factor
					|| line.length() >= line3.length() * factor || line.length() >= line4.length() * factor;
		}
	}*/

	/**
	 * Returns true if the vertex can be collapsed, that is the vertex can be removed
	 * by removing one edge and collapsing the other two. The vertex has to be at
	 * the boundary and has to have degree equal to three. We say that this vertex
	 * can break under the pressure of the forces.
	 *
	 * @param vertex the vertex
	 * @return true if the vertex can be collapsed / break.
	 */
	private boolean canBreak(@NotNull final V vertex) {
		if(allowVertexCollapse) {
			return getMesh().isAtBoundary(vertex) && getMesh().degree(vertex) == 3;
		}
    	return false;
	}

	/**
	 * Returns true if the vertex should be collapsed. That is the case if the resulting force
	 * acting on the vertex is low but the sum of all absolute partial forces is high. An analogy
	 * might be that the vertex breaks under the pressure of the forces.
	 *
	 * @param vertex the vertex which is tested.
	 * @return true if the vertex breaks under the pressure of the forces, otherwise false.
	 */
	private boolean isBreaking(@NotNull final V vertex) {
		double force = getForce(vertex).distanceToOrigin();
		P point = getMesh().getPoint(vertex);
		return point.getAbsoluteForce() > 0 && force / point.getAbsoluteForce() < Parameters.MIN_FORCE_RATIO;
	}


    /**
     * Computes the projection of Projects vertices. The projection acts towards the boundary of the boundary.
     * EikMesh projects only boundary vertices. Furthermore, to improve the convergence rate EikMesh additionally
     * projects vertices which are inside if the projection is inside a valid circle segment.
     *
     * @param vertex the vertex might be projected
     */
    private VPoint computeProjection(@NotNull final V vertex) {
    	// we only project boundary vertices back
	    if(getMesh().isAtBoundary(vertex)) {

		    P position = getMesh().getPoint(vertex);
		    double distance = distanceFunc.apply(position);

		    double x = position.getX();
		    double y = position.getY();

		    // the gradient (dx, dy)
		    double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps, 0))) - distance) / deps;
		    double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0, deps))) - distance) / deps;

		    double projX = dGradPX * distance;
		    double projY = dGradPY * distance;

		    double newX = x - projX;
		    double newY = y - projY;

	    	// back projection towards the inside if the point is outside
	    	if(isOutside(position, distanceFunc)) {
			    return new VPoint(newX, newY);
		    }
		    // back projection towards the inside if the point is inside (to improve the convergence rate of the algorithm)
		    else if(isInsideProjectionValid(vertex, newX, newY)) {
			    return new VPoint(newX, newY);
		    }
	    }

	    return new VPoint(vertex.getX(), vertex.getY());
    }

	/**
	 * Tests if a point is outside which is determined by the <tt>distanceFunc</tt>.
	 *
	 * @param point         the point of interest
	 * @param distanceFunc  the distance function which defines inside and outside
	 *
	 * @return true if the point is outside, false otherwise
	 */
	private boolean isOutside(@NotNull final IPoint point, @NotNull final IDistanceFunction distanceFunc) {
		return distanceFunc.apply(point) > 0;
    }

	/**
	 * Tests if the inside projection is valid which is the case if the angle at the vertex (at the boundary)
	 * is greater than 180 degree or the result of the projection lies inside the segment spanned by the
	 * vertex and its two neighbouring border vertices.
	 *
	 * @param vertex    the vertex
	 * @param newX      x-coordinate of the new position (after projection)
	 * @param newY      y-coordinate of the new position (after projection)
	 *
	 * @return true if the inside projection is valid, false otherwise
	 */
	private boolean isInsideProjectionValid(@NotNull final V vertex, final double newX, final double newY) {
	    Optional<E> boundaryEdgeOpt = getMesh().getBoundaryEdge(vertex);

	    if(!boundaryEdgeOpt.isPresent()) {
	    	return false;
	    }
	    else {
	    	// TODO: if the algorithm runs in parallel this might lead to unexpected results!
	    	E boundaryEdge = boundaryEdgeOpt.get();
		    VPoint p = getMesh().toPoint(vertex);
		    VPoint q = getMesh().toPoint(getMesh().getNext(boundaryEdge));
		    VPoint r = getMesh().toPoint(getMesh().getPrev(boundaryEdge));
		    double angle = GeometryUtils.angle(r, p, q);
		    return angle > Math.PI || (GeometryUtils.isLeftOf(r, p, newX, newY) && GeometryUtils.isLeftOf(p, q, newX, newY));
	    }
    }

	/**
	 * Computes the anchor to vertex partial relation. This relation gives some
	 * vertices their anchor point. If a vertex has an anchor point it will be driven
	 * towards this point (instead of the normal movement).
	 */
	private void computeAnchorPointRelation() {
		Set<VPoint> ancherPoints = generateAnchorPoints(shapes);
		ancherPoints.addAll(fixPoints);
		for(VPoint fixPoint : ancherPoints) {
			V closest = null;
			double distance = Double.MAX_VALUE;
			for(V vertex : getMesh().getVertices()) {
				if (closest == null || distance > vertex.distance(fixPoint)) {
					closest = vertex;
					distance = vertex.distance(fixPoint);
				}
			}
			fixPointRelation.put(closest, fixPoint);
		}
	}

    /**
     * Flips all edges which do not fulfill the Delaunay criterion and therefore being illegal.
     * Note that this is not a recursive flipping and therefore the result might not be a
     * Delaunay triangulation. However, due to the nature of the EikMesh algorithm (high quality initial mesh)
     * the triangulation is Delaunay in almost all cases and if not it is almost Delaunay.
     *
     * @return true, if any flip was necessary, false otherwise.
     */
    private boolean flipEdges() {
	    if(runParallel) {
	        streamEdges().filter(e -> getTriangulation().isIllegal(e)).forEach(e -> getTriangulation().flipSync(e));
        }
        else {
		    streamEdges().filter(e -> getTriangulation().isIllegal(e)).forEach(e -> getTriangulation().flip(e));
        }
        return false;
    }

    /**
     * Computation of the factor which transforms relative edge length into absolute ones.
     */
    private double computeEdgeScalingFactor(@NotNull final IEdgeLengthFunction edgeLengthFunc) {
        double edgeLengthSum = streamEdges()
                .map(edge -> getMesh().toLine(edge))
                .mapToDouble(line -> line.length())
                .sum();

        double desiredEdgeLenSum = streamEdges()
                .map(edge -> getMesh().toLine(edge))
                .map(line -> line.midPoint())
                .mapToDouble(midPoint -> edgeLengthFunc.apply(midPoint)).sum();
        return Math.sqrt((edgeLengthSum * edgeLengthSum) / (desiredEdgeLenSum * desiredEdgeLenSum));
    }


    // helper methods
    private Stream<E> streamEdges() {
        return runParallel ? getMesh().streamEdgesParallel() : getMesh().streamEdges();
    }

    private Stream<V> streamVertices() {
        return runParallel ? getMesh().streamVerticesParallel() : getMesh().streamVertices();
    }

	/**
	 * Returns true if and only if the vertex {@link V} is a fix point.
	 *
	 * @param vertex the vertex of interest
	 * @return true if and only if the vertex {@link V} is a fix point.
	 */
	private boolean isFixedVertex(final V vertex) {
		return getMesh().getPoint(vertex).isFixPoint() || nonEmptyBaseMode && getMesh().isAtBoundary(vertex);
	}

	/**
	 * Returns the force which is currently i.e. which was computed by
	 * {@link GenEikMesh#computeForce(IVertex)} applied to the vertex.
	 *
	 * @param vertex the vertex of interest
	 * @return the force which is currently applied to the vertex
	 */
	private IPoint getForce(final V vertex) {
		return getMesh().getPoint(vertex).getVelocity();
	}

	/**
	 * Applies the force of the vertex to the vertex which results
	 * in an displacement of the vertex.
	 *
	 * @param vertex the vertex of interest
	 */
	private void applyForce(final V vertex) {
		IPoint velocity = getForce(vertex);
		IPoint movement = velocity.scalarMultiply(delta);
		getMesh().getPoint(vertex).add(movement);
	}

	/**
	 * Computes the set of anchor points. An anchor point replaces fix points in EikMesh.
	 * Instead of inserting fix points EikMesh pushes (via forces) close points of
	 * an anchor point towards this anchor point. For each shape the the points of the
	 * path defining the shape will be added to the set of anchor points.
	 *
	 * @param shapes a list of shapes.
	 * @return the set of anchor points
	 */
	private Set<VPoint> generateAnchorPoints(@NotNull final Collection<? extends  VShape> shapes) {
		return shapes.stream()
				.flatMap(shape -> shape.getPath().stream())
				.filter(p -> bound.contains(p))
				.collect(Collectors.toSet());
	}

	/**
	 * Removes all faces neighbouring a boundary which can and should be removed.
	 *
	 * This takes O(n) time where n is the number of removed faces which will be consumed.
	 */
	private void removeFacesAtBoundary() {
		//Predicate<F> isOutside = f -> distanceFunc.apply(getMesh().toTriangle(f).midPoint()) > 0;
		Predicate<F> isSeparated = f -> getMesh().isSeparated(f);
		Predicate<F> isInvalid = f -> !getTriangulation().isValid(f);
		Predicate<F> isOfLowQuality = f -> faceToQuality(f) < Parameters.MIN_TRIANGLE_QUALITY && !isShortBoundaryEdge(f);
		Predicate<F> isBoundary = f -> getMesh().isBoundary(f);

		Predicate<F> mergePredicate = isSeparated.or(isInvalid)/*.or(isOutside)*/.or(isOfLowQuality);
		try {
			getTriangulation().removeFacesAtBoundary(mergePredicate, isBoundary);
		} catch (IllegalMeshException e) {
			log.error("error!");
		}
	}

	/**
	 * <p>Shrinks the border such that there are no more triangles outside the boundary i.e. where the distance is positive.
	 * Note the border is part of the whole boundary which is defined by the border and the holes.</p>
	 */
	private void shrinkBorder() {
		Predicate<F> removePredicate = face -> distanceFunc.apply(getTriangulation().getMesh().toTriangle(face).midPoint()) > 0;
		getTriangulation().shrinkBorder(removePredicate, true);
	}

	/**
	 * <p>Creates holes everywhere where the distance function is positive. Neighbouring holes will be merged.</p>
	 */
	private void createHoles() {
		List<F> faces = getTriangulation().getMesh().getFaces();
		for(F face : faces) {
			if(!getTriangulation().getMesh().isDestroyed(face) && !getTriangulation().getMesh().isHole(face)) {
				getTriangulation().createHole(face, f -> distanceFunc.apply(getTriangulation().getMesh().toTriangle(f).midPoint()) > 0, true);
			}
		}
	}


	private boolean isShortBoundaryEdge(@NotNull final F face) {
		E edge = getMesh().getBoundaryEdge(face).get();
		// corner => can be deleted

		VLine l1 = getMesh().toLine(edge);
		VLine l2 = getMesh().toLine(getMesh().getNext(edge));
		VLine l3 = getMesh().toLine(getMesh().getPrev(edge));

		if(l1.length() < l2.length() || l1.length() < l3.length()) {
			return true;
		}

		return false;
	}

	public static IDistanceFunction createDistanceFunction(@NotNull final VPolygon boundingBox, @NotNull final Collection<VPolygon> constrains) {
		IDistanceFunction distanceFunction = IDistanceFunction.create(boundingBox, constrains);
		PMesh<IPotentialPoint, Double, Double> mesh = new PMesh<>((x, y) -> new MeshExamples.PotentialPoint(x, y));
		PRuppertsTriangulator<IPotentialPoint, Double, Double> ruppertsTriangulator = new PRuppertsTriangulator<>(
				boundingBox,
				constrains,
				mesh,
				20,
				true);
		IIncrementalTriangulation<IPotentialPoint, Double, Double, PVertex<IPotentialPoint, Double, Double>, PHalfEdge<IPotentialPoint, Double, Double>, PFace<IPotentialPoint, Double, Double>> triangulation = ruppertsTriangulator.generate();
		for(IPotentialPoint point : mesh.getPoints()) {
			point.setPotential(distanceFunction.apply(point));
		}

		PMeshPanel<IPotentialPoint, Double, Double> panel = new PMeshPanel<>(mesh, 1000, 1000);
		panel.display("dist func.");

		IDistanceFunction approxDistance = p -> {
			Optional<PFace<IPotentialPoint, Double, Double>> optFace = triangulation.locateFace(p.getX(), p.getY());
			PFace<IPotentialPoint, Double, Double> face = optFace.get();
			if(mesh.isHole(face)) {
				return -mesh.toPolygon(face).distance(p);
			}
			else if(mesh.isBoundary(face)) {
				return mesh.toPolygon(face).distance(p);
			}
			else {
				return InterpolationUtil.barycentricInterpolation(triangulation.getMesh().getPoints(face), p.getX(), p.getY());
			}
		};
		return approxDistance;
	}

	public void setDistanceFunc(@NotNull final IDistanceFunction distanceFunc) {
		this.distanceFunc = distanceFunc;
	}

	public void setEdgeLengthFunc(@NotNull final IEdgeLengthFunction edgeLengthFunc) {
		this.edgeLengthFunc = edgeLengthFunc;
	}

	/*private void removeTrianglesInsideHoles() {
		List<F> holes = triangulation.getMesh().getHoles();
		Predicate<F> mergeCondition = f -> !triangulation.getMesh().isBoundary(f) && distanceFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0;
		for(F face : holes) {
			triangulation.mergeFaces(face, mergeCondition, true);
		}
	}

	private void removeTrianglesInsideObstacles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distanceFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
			}
		}
	}

	public void removeTrianglesOutsideBBox() {
		triangulation.shrinkBorder(f -> distanceFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
	}*/



	/*private void removeBoundaryLowQualityTriangles() {

		List<F> holes = triangulation.getMesh().getHoles();


		Predicate<F> mergeCondition = f ->
				(!triangulation.getMesh().isDestroyed(f) && !triangulation.getMesh().isBoundary(f) && triangulation.getMesh().isAtBoundary(f)) // at boundary
				&& (!triangulation.isValid(f) || (isCorner(f) || !isShortBoundaryEdge(f)) && faceToQuality(f) < Parameters.MIN_TRIANGLE_QUALITY) // bad quality
		;

		for(F face : holes) {
			List<F> neighbouringFaces = getMesh().streamEdges(face).map(e -> getMesh().getTwinFace(e)).collect(Collectors.toList());
			for (F neighbouringFace : neighbouringFaces) {
				if (mergeCondition.test(neighbouringFace)) {
					triangulation.removeEdges(face, neighbouringFace, true);
				}
			}
		}

		List<F> neighbouringFaces = getMesh().streamEdges(getMesh().getBorder()).map(e -> getMesh().getTwinFace(e)).collect(Collectors.toList());
		for (F neighbouringFace : neighbouringFaces) {
			if (mergeCondition.test(neighbouringFace)) {
				triangulation.removeEdges(getMesh().getBorder(), neighbouringFace, true);
			}
		}

		//triangulation.mergeFaces(getMesh().getBorder(), mergeCondition, true);
	}*/
}
