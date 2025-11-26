package org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.IllegalMeshException;
import org.vadere.meshing.mesh.inter.IEdgeContainerBoolean;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertexContainerBoolean;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.meshing.mesh.iterators.EdgeIterator;
import org.vadere.meshing.mesh.iterators.EdgeIteratorReverse;
import org.vadere.meshing.mesh.triangulation.improver.IMeshImprover;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Parameters;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRivaraRefinement;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenVoronoiSegmentInsertion;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformRefinementTriangulatorSFC;
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
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 */
public class GenEikMesh<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IMeshImprover<V, E, F>, ITriangulator<V, E, F> {

	private IRefiner<V, E, F> refiner;
	private IIncrementalTriangulation<V, E, F> triangulation;

	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction edgeLengthFunc;
	private VRectangle bound;
	private Collection<? extends IPoint> fixPoints;
	private double scalingFactor;
	private double deps;
	private static final int MAX_STEPS = Parameters.MAX_NUMBER_OF_STEPS;
	private int nSteps;
	private double initialEdgeLen;
	private double maxMovement;
	private double quality;
	private double dQuality = Double.POSITIVE_INFINITY;

	// Persson-Strang smothing function
	private Function<Double, Double> f = d -> Math.max(1-d, 0);

	// Bosson-Heckbert smoothing function
	//private Function<Double, Double> f = d -> 0.5 * (1-Math.pow(d, 4)) * Math.exp(-Math.pow(d, 4));

	private boolean initialized = false;
	private boolean runParallel = false;
	private boolean profiling = false;
	private double minDeltaTravelDistance = 0.0;
	private double delta = Parameters.DELTAT;
	private Collection<? extends VShape> shapes;
	private Map<V, VLine> pointToSlidingLine;
	private boolean nonEmptyBaseMode;
	private int nStepQualityTest = 10;
	private boolean useSlidingLines;

	//TODO: local connectifity changes?
	/*private Set<V> frozenVertices;
	private Set<F> frozenFaces;
	private LinkedList<F> poorFaces;*/

	// different options

	// spilts long boundary edges
	private boolean allowEdgeSplits = true;

	// edge collapse of shortest edges of poor non-boundary triangles
	private boolean allowEdgeCollapse = false;

	// removes short bounary edges
	private boolean allowVertexCollapse = true;

	// removes low quality boundary faces
	private boolean allowFaceCollapse = false;

	// TODO: removes low quality boundary faces which is similar to allowFaceCollapse
	private boolean removeLowBoundaryTriangles = false;
	private boolean removeOutsideTriangles = false;
	private boolean useVirtualEdges = true;

	// if no PSLG set this to be true
	private boolean smoothBorder;

	// counter acts long boundary edges
	private boolean freezeVertices = false;
	//private boolean splitFaces = true;
	//private boolean useFixPoints = false;

	// only for logging
	private static final Logger log = Logger.getLogger(GenEikMesh.class);

	static {
		//log.setDebug();
	}


	// properties saved for different mesh elements i.e. vertices, edges and faces
	private static final String propFixPoint = "fixPoint";
	private static final String propConstrained = "constrained";
	private static final String propVelocityX = "velocityX";
	private static final String propVelocityY = "velocityY";
	private static final String propAbsVelocity = "absVelocity";

	private final IVertexContainerBoolean<V, E, F> fixpointC;
	private final IEdgeContainerBoolean<V, E, F> constraintC;
	private final IVertexContainerDouble<V, E, F> velocityXC;
	private final IVertexContainerDouble<V, E, F> velocityYC;
	private final IVertexContainerDouble<V, E, F> absVelocityC;

	/**
	 * Constructor to use EikMesh on an existing {@link org.vadere.meshing.mesh.inter.ITriangulation}, that is
	 * EikMesh uses this triangulation as a bases. It will refineSimplex2D the triangulation by using a longest edge
	 * split strategy {@link GenRivaraRefinement} to generate desired edge length determined by
	 * len(p) = <tt>edgeLengthFunc(p)</tt> if <tt>refine</tt> is set to <tt>true</tt>. Since there is no {@link IDistanceFunction} function
	 * the geometry i.e. the boundary and holes are defined by the given triangulation itself. Therefore, the algorithm does
	 * not project points but uses slide points only.
	 *
	 * Assumption:
	 * <ol>
	 *     <ul><tt>edgeLengthFunc</tt> should be something like <tt>edgeLengthFunc</tt>(p) = minEdgeLen + f(p) and should be >= minEdgeLen everywhere!</ul>
	 *     <ul><tt>triangulation</tt> should be a valid triangulation</ul>
	 * </ol>
	 *
	 *
	 * @param edgeLengthFunc    the desired edge length function
	 * @param triangulation     a valid triangulation
	 * @param refine            determines whether the given triangulation will be refined or it will not.
	 */
	public GenEikMesh(
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			final boolean refine) {
		IMeshDataStorage<V, E, F> dataStorage = triangulation.getMeshDataStorage();
		this.fixpointC = dataStorage.getBooleanVertexContainer(propFixPoint);
		this.constraintC = dataStorage.getBooleanEdgeContainer(propConstrained);
		this.velocityXC = dataStorage.getDoubleVertexContainer(propVelocityX, triangulation.getMesh());
		this.velocityYC = dataStorage.getDoubleVertexContainer(propVelocityY, triangulation.getMesh());
		this.absVelocityC = dataStorage.getDoubleVertexContainer(propAbsVelocity, triangulation.getMesh());
		this.shapes = new ArrayList<>();
		this.bound = null;
		this.edgeLengthFunc = edgeLengthFunc;
		this.nSteps = 0;
		//this.fixPointRelation = new HashMap<>();
		this.triangulation = triangulation;

		this.distanceFunc = null;
		this.nonEmptyBaseMode = true;
		this.fixPoints = Collections.EMPTY_LIST;
		this.pointToSlidingLine = new HashMap<>();
		//this.frozenFaces = new HashSet<>();
		//this.frozenVertices = new HashSet<>();
		//this.poorFaces = new LinkedList<>();
		this.triangulation.setCanIllegalPredicate(e -> true);
		this.maxMovement = Double.NEGATIVE_INFINITY;
		this.triangulation.getMesh().edges().streamBoundaryEdges()
				.map(e -> triangulation.getMesh().vertices().getEndOf(e)).forEach(v -> setFixPoint(v, true));

		if(refine) {
			this.refiner = new GenVoronoiSegmentInsertion<>(triangulation, p -> edgeLengthFunc.apply(p));
		} else {
			this.refiner = null;
		}
		this.useSlidingLines = true;
		this.smoothBorder = false;

	}

	/**
	 * Constructor to use EikMesh on an existing {@link org.vadere.meshing.mesh.inter.ITriangulation}, that is
	 * EikMesh uses this triangulation as a bases. It will not refine the the triangulation further.
	 * Since there is no {@link IDistanceFunction} function the geometry i.e. the boundary and holes are defined by the given triangulation itself.
	 * Therefore, the algorithm does not project points but uses slide points only.
	 *
	 * Assumption:
	 * <ol>
	 *     <ul><tt>edgeLengthFunc</tt> should be something like <tt>edgeLengthFunc</tt>(p) = minEdgeLen + f(p) and should be >= minEdgeLen everywhere!</ul>
	 *     <ul><tt>triangulation</tt> should be a valid triangulation</ul>
	 * </ol>
	 *
	 *
	 * @param edgeLengthFunc    the relative desired edge length function
	 * @param triangulation     a valid triangulation
	 */
	public GenEikMesh(
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation) {
		this(edgeLengthFunc, triangulation, false);
	}

	/**
	 * Constructor to use EikMesh on an existing {@link org.vadere.meshing.mesh.inter.ITriangulation}, that is
	 * EikMesh uses this triangulation as a bases. It will refineSimplex2D the triangulation by using a longest edge
	 * split strategy {@link GenRivaraRefinement} to generate desired edge length determined by
	 * len(p) = <tt>edgeLengthFunc(p)</tt>. Since a {@link IDistanceFunction} function is given projection is used (and no sliding points).
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
	 * @param triangulation     a valid triangulation
	 * @param refine            determines whether the given triangulation will be refined or it will not.
	 */
	public GenEikMesh(
			@NotNull final IDistanceFunction distanceFunc,
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			final boolean refine) {
		this.shapes = new ArrayList<>();
		this.bound = null;
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.nSteps = 0;
		//this.fixPointRelation = new HashMap<>();
		this.nonEmptyBaseMode = true;
		this.fixPoints = Collections.EMPTY_LIST;
		this.pointToSlidingLine = new HashMap<>();
		this.maxMovement = Double.NEGATIVE_INFINITY;


		if(refine) {
			this.refiner = new GenVoronoiSegmentInsertion<>(triangulation, p -> edgeLengthFunc.apply(p));
		} else {
			this.refiner = null;
		}

		this.useSlidingLines = false;
		this.smoothBorder = true;

		IMeshDataStorage<V, E, F> dataStorage = triangulation.getMeshDataStorage();
		this.fixpointC = dataStorage.getBooleanVertexContainer(propFixPoint);
		this.constraintC = dataStorage.getBooleanEdgeContainer(propConstrained);
		this.velocityXC = dataStorage.getDoubleVertexContainer(propVelocityX, triangulation.getMesh());
		this.velocityYC = dataStorage.getDoubleVertexContainer(propVelocityY, triangulation.getMesh());
		this.absVelocityC = dataStorage.getDoubleVertexContainer(propAbsVelocity, triangulation.getMesh());
	}

	/**
	 * Constructor to use EikMesh to construct the whole new triangulation based on a given geometry
	 * defined by a {@link IDistanceFunction} and additionally (optional) by some {@link VShape}s, where
	 * each of the elements in <tt>shapes</tt> is part of the (outside) boundary area.
	 * The algorithm will generate sliding points on the base of the given {@link VShape}s and will project
	 * based on the given {@link IDistanceFunction}.
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
			@NotNull final Collection<? extends IPoint> fixPoints,
			final double initialEdgeLen,
			@NotNull final VRectangle bound,
			@NotNull final Collection<? extends VShape> shapes,
			@NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshSupplier) {
		this.shapes = shapes;
		this.bound = bound;
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.nSteps = 0;
		//this.fixPointRelation = new HashMap<>();
		this.nonEmptyBaseMode = false;
		this.fixPoints = fixPoints;
		this.pointToSlidingLine = new HashMap<>();
		this.useSlidingLines = !shapes.isEmpty();
		this.smoothBorder = true;
		this.removeOutsideTriangles = true;
		this.refiner = new GenUniformRefinementTriangulatorSFC(
				meshSupplier,
				bound,
				shapes,
				edgeLengthFunc,
				initialEdgeLen,
				distanceFunc,
				generateFixPoints());

		IMeshDataStorage<V, E, F> dataStorage = refiner.getMeshDataStorage();
		this.fixpointC = dataStorage.getBooleanVertexContainer(propFixPoint);
		this.constraintC = dataStorage.getBooleanEdgeContainer(propConstrained);
		this.velocityXC = dataStorage.getDoubleVertexContainer(propVelocityX, triangulation.getMesh());
		this.velocityYC = dataStorage.getDoubleVertexContainer(propVelocityY, triangulation.getMesh());
		this.absVelocityC = dataStorage.getDoubleVertexContainer(propAbsVelocity, triangulation.getMesh());
	}

	public GenEikMesh(
			@NotNull final IDistanceFunction distanceFunc,
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			@NotNull final VRectangle bound,
			@NotNull final Collection<? extends VShape> shapes,
			@NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshSupplier) {
		this(distanceFunc, edgeLengthFunc, Collections.EMPTY_LIST, initialEdgeLen, bound, shapes, meshSupplier);
	}

	public GenEikMesh(
			@NotNull final IDistanceFunction distanceFunc,
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			@NotNull final VRectangle bound,
			@NotNull final Supplier<ITriangleMeshBuilder<V, E, F>>meshSupplier) {
		this(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, Collections.EMPTY_LIST, meshSupplier);
	}

	public GenEikMesh(@NotNull final VPolygon segmentBound,
	                  final double initialEdgeLen,
	                  @NotNull final Collection<? extends VShape> shapes,
	                  @NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshSupplier){
		this(new DistanceFunction(segmentBound, shapes), p -> initialEdgeLen, initialEdgeLen, GeometryUtils.boundRelative(segmentBound.getPoints()), shapes, meshSupplier);
	}

	@Override
	public ITriangleMesh<V, E, F> getMesh() {
		return ITriangulator.super.getMesh();
	}

	/**
	 * This should be called if the edge length function or the distance function changes.
	 */
	private void reset() {
		this.maxMovement = Double.POSITIVE_INFINITY;
		this.nSteps = 0;
		this.dQuality = Double.POSITIVE_INFINITY;
	}

	/**
	 * This will initialize the mesh before any force is computed and any vertex is displaced.
	 */
	public void initialize() {
		if(hasRefiner() && !refiner.isFinished()) {
			refiner.refine();
		} else {
			initialEdgeLen = getMesh().edges()
					.stream()
					.map(e -> getMesh().edges().toLine(e).length()).min(Double::compareTo).orElse(Double.POSITIVE_INFINITY);
			//deps = 2.22e-16 * initialEdgeLen;
			deps = 0.0001 * initialEdgeLen;

			computeFixPoints();
			if(hasRefiner()) {
				refiner.getConstrains().forEach(e -> setConstraint(e, true));
			}

			if(useSlidingLines) {
				computeSlidingLines();
			}
			quality = getQuality();
			initialized = true;
		}
	}

	private boolean useSlidingLines() {
		return useSlidingLines;
	}

	/**
	 * Returns the fix points, i.e. points that can not move.
	 *
	 * @return the collection of fix points
	 */
	public Collection<V> getFixVertices() {
		return refiner.getFixPoints();
	}

	/**
	 * Returns true if the initial mesh has been generated, false otherwise
	 *
	 * @return true if the initial mesh has been generated, false otherwise
	 */
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate() {
		return generate(true);
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		while (!isInitialized()) {
			initialize();
		}

		while (!isFinished()) {
			improve();
		}

		if(finalize) {
			finish();

			IMeshBuilder<V, E, F> mutableMesh = getTriangulation().getMeshBuilder();
			mutableMesh.getOptimizer().garbageCollection();
		}

		return getTriangulation();
	}

	/**
	 * Returns true if the final mesh has been constructed, that is if the algorithm converges or the mesh quality is good enough.
	 *
	 * @return true if the final mesh has been constructed, false otherwise
	 */
	public boolean isFinished() {
		synchronized (getMeshBuilder()) {
			boolean converged = dQuality < Parameters.qualityConvergence;
			return isInitialized() && quality >= Parameters.qualityMeasurement && converged || (maxMovement > 0 && maxMovement / initialEdgeLen < Parameters.DPTOL) || nSteps >= MAX_STEPS;
		}
	}

	public ITriangleMeshBuilder<V, E, F> getMeshBuilder() {
		return getTriangulation().getMeshBuilder();
	}

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return getTriangulation().getMeshDataStorage();
	}

	/**
	 * Convert vertices to fix points if their surrounding faces have a good enough quality.
	 */
	private void freezeVertices() {
		if(freezeVertices) {
			getMesh().vertices().stream().filter(v -> getMesh().faces().streamOf(v).filter(f -> !getMesh().faces().isBoundary(f)).allMatch(f -> faceToQuality(f) > Parameters.qualityMeasurement)).forEach(v -> setFixPoint(v, true));
			getMesh().faces().stream().filter(f -> getMesh().vertices().streamVerticesOf(f).allMatch(v -> isFixPoint(v))).forEach(f -> this.getMeshDataStorage().setBooleanData(f, "frozen", true));
		}
	}

	@Override
	public void improve() {
		synchronized (getMeshBuilder()) {
			//System.out.println(getMesh().streamVertices().filter(v -> isSlidePoint(v)).count());
			if(!isInitialized()) {
				initialize();
			}
			else {
				maxMovement = Double.NEGATIVE_INFINITY;
				// geometry is defined by a PSLG
				if(removeLowBoundaryTriangles) {
					removeFacesAtBoundary();
				} else if(removeOutsideTriangles) {
					shrinkBoundary();
				}

				if(hasDistanceFunction() && smoothBorder) {
					getMeshBuilder().changeConnectivity().smoothBoundary(distanceFunc, v -> isFixPoint(v));
				}

				if(allowFaceCollapse) {
					getMeshBuilder().changeConnectivity().collapseBoundaryFaces(
							f -> true,
							e -> !isFixPoint(getMesh().vertices().getEndOf(e)) && getMeshBuilder().getMesh().readConnectivity().isLargeAngle(e, Parameters.MAX_COLLAPSE_ANGLE),
							v -> {
								if(useSlidingLines) {
									E edge = getMesh().edges().getBoundaryEdge(v).get();
									if(isSlidePoint(getMesh().vertices().getEndOf(getMesh().edges().getNext(edge))) && isSlidePoint(getMesh().vertices().getEndOf(getMesh().edges().getPrev(edge)))) {
										pointToSlidingLine.put(v, pointToSlidingLine.get(getMesh().vertices().getEndOf(getMesh().edges().getNext(edge))));
									}
								}
							});
				}
				/*if(splitFaces) {
					for(F f : getMesh().getFaces()) {
						if(getTriangulation().faceToQuality(f) < 0.4) {
							VTriangle triangle = getMesh().toTriangle(f);
							VPoint circumCenter = triangle.getCircumcenter();
							if(triangle.contains(circumCenter)) {
								getTriangulation().insertVertex(getMesh().createPoint(circumCenter.getX(), circumCenter.getY()), f);
							}
						}
					}
				}*/
				//if(nSteps % 5 == 0) {
				flipEdges();

				//}

				scalingFactor = computeEdgeScalingFactor(edgeLengthFunc);
				computeVertexForces();
				//computeBoundaryForces(); // replaced by virtual edges!
				updateEdges();
				updateVertices();
				freezeVertices();
				nSteps++;


				double tmpQuality = getQuality();
				if(nSteps % nStepQualityTest == 0) {
					dQuality = Math.abs(tmpQuality-quality);
					quality = tmpQuality;
				}
				assert getMesh().isValid();
				log.debug("quality (" + nSteps + "):" + tmpQuality);
			}
		}
	}

	/**
	 * Returns true if the algorithm is based on a distance function. Otherwise it is based on a PSLG and will return false.
	 *
	 * @return true if the algorithm is based on a distance function, false otherwise
	 */
	private boolean hasDistanceFunction() {
		return distanceFunc != null;
	}

	/**
	 * Finishes the mesh after the improvement has ended.
	 */
	public void finish() {
		if(hasDistanceFunction()){
			try {
				removeFacesOutside(distanceFunc);
			} catch (IllegalMeshException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return hasRefiner() ? refiner.getTriangulation() : triangulation;
	}

	@Override
	public synchronized Collection<VTriangle> getTriangles() {
		return getTriangulation().streamTriangles().collect(Collectors.toList());
	}

	/**
	 * computes the edge forces / velocities for all half-edge i.e. for each edge twice!
	 */
	private void computeForces() {
		streamEdges().forEach(e -> getForce(getMesh().edges().endToPoint(e), getMesh().edges().endToPoint(getMesh().edges().getPrev(e))));
	}

	/**
	 * computes the edge forces / velocities for all half-edge i.e. for each edge twice!
	 */
	private void computeBoundaryForces() {
		getMesh().edges().streamBoundaryEdges().forEach(e -> computeBoundaryForces(e));
	}

	/**
	 * computes the vertex forces / velocities for all vertices
	 */
	private void computeVertexForces() {
		streamVertices().forEach(v -> computeForce(v));
	}

	/**
	 * Computes and sets the overall force acting on a vertex. This force is determined by all adjacent real edges and
	 * in case of points 'close' to the boundary by a virtual edge.
	 *
	 * @param vertex the vertex of interest
	 */
	private void computeForce(final V vertex) {
		// TODO: Get rid of IPoint
		IPoint p1 = getMesh().vertices().toMutablePoint(vertex);
		boolean isAtBoundary = isBoundary(vertex);

		for(E edge : getMesh().edges().getAllOf(vertex)) {

			// (1) force computation for "real" edges
			V v2 = getMesh().vertices().getEndOf(getMesh().edges().getTwin(edge));
			IPoint p2 = getMesh().vertices().toMutablePoint(v2);
			VPoint force = getForce(getMesh().vertices().toPoint(p1), getMesh().vertices().toPoint(p2));

			increaseVelocity(vertex, force);
			increaseAbsVelocity(vertex, force.distanceToOrigin());

			// (2) force computation for "virtual edges"
			if(useVirtualEdges && !isAtBoundary) {
				E prev = getMesh().edges().getPrev(edge);
				if(getMesh().edges().isAtBoundary(prev) || isSlidePoint(getMesh().vertices().getEndOf(prev))) {
					VPoint q1 = getMesh().edges().endToPoint(prev);
					VPoint q2 = getMesh().edges().endToPoint(getMesh().edges().getPrev(prev));

					VPoint m = GeometryUtils.projectOntoLine(p1.getX(), p1.getY(), q1.getX(), q1.getY(), q2.getX(), q2.getY());
					//VPoint m = q1.add(q2).scalarMultiply(0.5);

					VPoint dir = m.subtract(p1).scalarMultiply(2);
					VPoint q3 = getMesh().vertices().toPoint(p1).add(dir);
					VPoint virtualForce = getForceVirtual(getMesh().vertices().toPoint(p1), q3);

					// only take the part of the force which act perpendicular to the boundary edge (q1, q2)
					//IPoint projection = GeometryUtils.projectOnto(virtualForce.getX(), virtualForce.getY(), q2.getX() - q1.getX(), q2.getY() - q1.getY());

					//virtualForce = virtualForce.subtract(projection);


					/*VPoint dir = m.subtract(p1).scalarMultiply(1.4);
					VPoint q3 = getMesh().toPoint(p1).add(dir);
					VPoint virtualForce = getForce(getMesh().toPoint(p1), q3);*/

					increaseVelocity(vertex, virtualForce);
					increaseAbsVelocity(vertex, virtualForce.distanceToOrigin());

				}
			}

		}
	}

	/**
	 * Computes the force for point p1 respect to point p2.
	 * @param p1 the point of interest
	 * @param p2 the second point
	 *
	 * @return the force for point p1 respect to point p2
	 */
	private VPoint getForce(@NotNull final VPoint p1, @NotNull final VPoint p2) {
		// TODO: Get rid of VPoint
		VPoint p1p2 = p1.subtract(p2);
		double len = p1p2.distanceToOrigin();
		double desiredLen = getDesiredEdgeLength(p1, p2);
		double ratio = len / desiredLen;
		double absForce = f.apply(ratio);
		VPoint force = p1p2.setMagnitude(absForce * desiredLen);
		return force;
	}

	/**
	 * Computes the force for point p1 respect to point p2.
	 * @param p1 the point of interest
	 * @param p2 the second point
	 *
	 * @return the force for point p1 respect to point p2
	 */
	private VPoint getForceVirtual(@NotNull final VPoint p1, @NotNull final VPoint p2) {
		// TODO: Get rid of VPoint
		VPoint p1p2 = p1.subtract(p2);
		double len = p1p2.distanceToOrigin();
		if(len <= GeometryUtils.DOUBLE_EPS) {
			return new VPoint(0, 0);
		}
		double desiredLen = 0.9 * Math.sqrt(3) * getDesiredEdgeLength(p1, p2);
		double ratio = len / desiredLen;
		double absForce = f.apply(ratio);
		VPoint force = p1p2.setMagnitude(absForce * desiredLen);
		return force;
	}

	/**
	 * Computes the desired edge length at a for a given edge defined by (p1, p2).
	 *
	 * @param p1 the first point of the edge
	 * @param p2 the second point of the edge
	 *
	 * @return the desired edge length
	 */
	private double getDesiredEdgeLength(@NotNull final VPoint p1, @NotNull final VPoint p2) {
		// TODO: Get rid of VPoint
		VPoint p = p1.add(p2).scalarMultiply(0.5);
		return getDesiredEdgeLength(p);
	}

	/**
	 * Computes the desired edge length at a given point <tt>p</tt>.
	 *
	 * @param p the point at which the desired edge length is computed
	 *
	 * @return the desired edge length at a given point
	 */
	private double getDesiredEdgeLength(@NotNull final IPoint p) {
		// TODO: Get rid of VPoint
		return edgeLengthFunc.apply(p) * Parameters.FSCALE * scalingFactor;
	}

	/**
	 * Unused: see (A Matlab mesh generator for the two-dimensional finite element method, doi: 10.1016/j.amc.2014.11.009)
	 *
	 * @param edge
	 */
	private void computeForcesBossan(final E edge) {
		V v1 = getMesh().vertices().getEndOf(edge);
		V v2 = getMesh().vertices().getEndOf(getMesh().edges().getPrev(edge));

		double len = Math.sqrt((v1.getX() - v2.getX()) * (v1.getX() - v2.getX()) + (v1.getY() - v2.getY()) * (v1.getY() - v2.getY()));
		double desiredLen = edgeLengthFunc.apply(new VPoint((v1.getX() + v2.getX()) * 0.5, (v1.getY() + v2.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;

		double lenDiff = Math.max(desiredLen - len, 0);
		double dVelX = (v1.getX() - v2.getX()) * (lenDiff / (len / desiredLen));
		double dVelY = (v1.getY() - v2.getY()) * (lenDiff / (len / desiredLen));
		increaseVelocityX(v1, dVelX);
		increaseVelocityY(v1, dVelY);
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
		/*
		 * (1) break / remove the vertex if the forces are to large / there is too much pressure
		 * (2) otherwise displace the vertex
		 */
		if(canBreak(vertex) && isBreaking(vertex)) {
			// TODO: if the algorithm runs in parallel this might lead to unexpected results! synchronized required!
			getMeshBuilder().changeConnectivity().collapse3DVertex(vertex, true);
		}
		else if(!isFixPoint(vertex)) {
			/*
			 * (2.1) if it is a sliding point which slides on the boundary, then let the point only move on its sliding line.
			 */
			if(isSlidePoint(vertex)) {
				VLine lineEdge = pointToSlidingLine.get(vertex);
				IPoint velocity = getForce(vertex);
				IPoint movement = velocity.scalarMultiply(delta);
				IPoint projection = GeometryUtils.projectOnto(movement.getX(), movement.getY(), lineEdge.getX2() - lineEdge.getX1(), lineEdge.getY2() - lineEdge.getY1());
				move(vertex, vertex.getX() + projection.getX(), vertex.getY() + projection.getY());
			}
			// (2.2) if it is not a sliding point, use the default movement and projection method.
			else{
				// (2.1) p_{k+1} = p_k + dt * F(p_k)
				applyForce(vertex);
				if(hasDistanceFunction()) {
					VPoint projection = computeProjection(vertex);
					move(vertex, projection.getX(), projection.getY());
				}
			}
		}
		setVelocityX(vertex, 0);
		setVelocityY(vertex, 0);
		setAbsVelocity(vertex, 0);
	}

	/**
	 * Moves the vertex to its new location (newX, newY) if and only if the movement is legal.
	 *
	 * @param vertex    the vertex of interest
	 * @param newX      the new x-coordinate
	 * @param newY      the new y-coordinate
	 */
	private boolean move(@NotNull final V vertex, double newX, double newY) {
		if(isLegalMove(vertex, newX, newY)) {
			double distance = GeometryUtils.length(vertex.getX() - newX, vertex.getY() - newY);
			getMeshBuilder().vertices().setCoords(vertex, newX, newY);
			if(maxMovement < distance) {
				maxMovement = distance;
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns true if and only if the vertex is a sliding point which means it can only slide on a so called sliding line.
	 *
	 * @param vertex the vertex of interest.
	 *
	 * @return true if and only if the vertex is a sliding point
	 */
	public boolean isSlidePoint(@NotNull V vertex) {
		boolean slidePoint = /*getMesh().isAtBoundary(vertex) &&*/ !isFixPoint(vertex) && pointToSlidingLine.containsKey(vertex);
		return slidePoint;
	}

	/**
	 * Tests if the new position is inside the 1-ring polygon.
	 *
	 * @param vertex the vertex which wants to be moved
	 * @param newX   x-coordinate of the new vertex position
	 * @param newY   y-coordinate of the new vertex position
	 *
	 * @return true if the movement is legal, false otherwise
	 */
	private boolean isLegalMove(@NotNull final V vertex, double newX, double newY) {
		// only test for early iterations!
		if(nSteps > Parameters.HIGHEST_LEGAL_TEST) {
			return true;
		}

		//if(vertex.distance(newX, newY) > GeometryUtils.DOUBLE_EPS) {

			// TODO: at the boundary vertices can still overtake each other.
			/*if(getMesh().isAtBoundary(vertex)) {
				E boundaryEdge = getMesh().getBoundaryEdge(vertex).get();
				V next = getMesh().getVertex(getMesh().getNext(boundaryEdge));
				V prev = getMesh().getVertex(getMesh().getPrev(boundaryEdge));

				IPoint dirN = next.subtract(vertex);
				IPoint dirP = vertex.subtract(prev);

				IPoint newDirN = next.subtract(new VPoint(newX, newY));
				IPoint newDirP = new VPoint(newX, newY).subtract(prev);

				double angleDifN = Math.abs(GeometryUtils.angleTo(dirN) - GeometryUtils.angleTo(newDirN));
				double angleDifP = Math.abs(GeometryUtils.angleTo(dirP) - GeometryUtils.angleTo(newDirP));

				if(angleDifN > Math.PI || angleDifP > Math.PI) {
					return false;
				}
			}*/

			return getMesh().edges().streamEdgesOf(vertex)
					.filter(e -> !getMesh().edges().isBoundary(e))
					.map(e -> getMesh().edges().getPrev(e))
					.allMatch(e -> getMesh().readConnectivity().isLeftOf(newX, newY, e));
		//}
		//return false;
	}

	/**
	 * unused.
	 */
	private void updateFaces() {
		if(nonEmptyBaseMode) {
			getMesh().faces().getAll().stream().forEach(f -> updateFace(f));
		}
	}

	/**
	 * unused
	 * @param face
	 */
	private void updateFace(@NotNull F face) {
		if(canBreak(face) && isBreaking(face)) {
			VPoint circumcenter = getMesh().faces().toCircumcenter(face);
			getMeshBuilder().changeConnectivity()
					.splitTriangle(face, getMeshBuilder().getMesh().createPoint(circumcenter.getX(), circumcenter.getY()), false);
		}
	}

	/**
	 * Updates all edges. Some of those edges might get split, some may collapse.
	 */
	private void updateEdges() {
		// edge splits
		getMesh().edges().getBoundaryEdges().forEach(e -> updateBoundaryEdge(e));
		if(allowEdgeCollapse) {
			for(E e : getMesh().edges().getAll()) {
				if(!getMesh().edges().isDestroyed(e) && !getMesh().edges().isAtBoundary(e)) {
					// edge collapse
					updateEdge(e);
				}
			}
		}
	}

	/**
	 * Collapses the edge if it is too short.
	 *
	 * @param edge edge of interest
	 *
	 * @return true if the edge is collapsed, false otherwise
	 */
	private boolean updateEdge(@NotNull final E edge) {
		if(getMesh().readConnectivity().isShortestHalfEdge(edge) && (faceToQuality(edge) < Parameters.MIN_COLLAPSE_QUALITY)) {
			V v1 = getMesh().vertices().getEndOf(edge);
			V v2 = getMesh().vertices().getTwin(edge);

			if(!isFixPoint(v1) || !isFixPoint(v2)) {

				if(isSlidePoint(v1) && isSlidePoint(v2) && !pointToSlidingLine.get(v1).equals(pointToSlidingLine.get(v2))) {
					return false;
				}

				VPoint newPosition;
				if(isFixPoint(v1)) {
					newPosition = new VPoint(v1.getX(), v1.getY());
				} else if(isFixPoint(v2)) {
					newPosition = new VPoint(v2.getX(), v2.getY());
				} else if(isBoundary(v1) || (isSlidePoint(v1) && !isSlidePoint(v2))) {
					newPosition = new VPoint(v1.getX(), v1.getY());
				} else if(isBoundary(v2) || (isSlidePoint(v2) && !isSlidePoint(v1))) {
					newPosition = new VPoint(v2.getX(), v2.getY());
				} else {
					newPosition = new VPoint((v1.getX() + v2.getX()) * 0.5, (v1.getY() + v2.getY()) * 0.5);
				}

				if(isLegalMove(getMesh().vertices().getEndOf(edge), newPosition.getX(), newPosition.getY()) &&
						isLegalMove(getMesh().vertices().getTwin(edge), newPosition.getX(), newPosition.getY())) {

					V v = getMeshBuilder().changeConnectivity().collapseEdge(edge, true);

					getMeshBuilder().vertices().setPoint(v, newPosition);

					if((isSlidePoint(v1) && !isFixPoint(v1))) {
						pointToSlidingLine.put(v, pointToSlidingLine.get(v1));
					} else if((isSlidePoint(v2) && !isFixPoint(v2))){
						pointToSlidingLine.put(v, pointToSlidingLine.get(v2));
					}

					if(isFixPoint(v1) || isFixPoint(v2)) {
						setFixPoint(v, true);
					}

					log.debug("edge collapse");
				}

				return true;
			}
		}
		return false;
	}

	/**
	 * Splits a boundary edge if necessary, i.e. if the boundary edge is very long.
	 *
	 * @param edge the edge
	 */
	private void updateBoundaryEdge(@NotNull final E edge) {
		if(canBreak(edge) && isBreaking(edge)) {
			boolean isConstrained = isConstrained(edge);
			//if(distanceFunc == null) {
			V v1 = getMesh().vertices().getEndOf(edge);
			V v2 = getMesh().vertices().getEndOf(getMesh().edges().getPrev(edge));
			VLine line;

			boolean isFixedSlicePoint = useSlidingLines && isFixPoint(v1) && isFixPoint(v2) && !hasDistanceFunction();
			boolean isSlicePoint = (useSlidingLines && (isSlidePoint(v1) || isSlidePoint(v2)));

			if(isSlicePoint) {
				if(pointToSlidingLine.containsKey(v1)) {
					line = pointToSlidingLine.get(v1);
				}
				else {
					line = pointToSlidingLine.get(v2);
				}
			} else if(isFixedSlicePoint) {
				line = getMesh().edges().toLine(edge);
			} else {
				line = null;
			}

			final boolean fisSlidePoint = isSlicePoint || isFixedSlicePoint;
			Pair<E, E> newEdges = getMeshBuilder().changeConnectivity().splitEdge(edge, false, v -> {
				setFixPoint(v, false);
				if(fisSlidePoint) {
					pointToSlidingLine.put(v, line);
				}
			});

			if(isConstrained) {
				setConstraint(newEdges.getLeft(), true);
				setConstraint(newEdges.getRight(), true);
			}
			/*} else {
				getTriangulation().splitEdge(edge, true, v -> setFixPoint(v, false));
			}*/
		}
	}

	/**
	 * unused
	 * @param face
	 * @return
	 */
	private boolean canBreak(@NotNull final F face) {
		return !getMesh().faces().isBoundary(face);
	}

	/**
	 * unused
	 * @param face
	 * @return
	 */
	private boolean isBreaking(@NotNull final F face) {
		if(faceToQuality(face) > 0.95) {
			E edge = getMesh().edges().getAnyOf(face);
			if(edgeLengthFunc.apply(getMesh().edges().toLine(edge).midPoint()) * 2.1 <= getMesh().edges().toLine(edge).length()) {
				VPoint circumcenter = getMesh().faces().toCircumcenter(face);
				return getMesh().readConnectivity().faceContains(circumcenter.getX(), circumcenter.getY(), face);
						//getMesh().toTriangle(face).contains(circumcenter);
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
		return allowEdgeSplits /*&& getMesh().isAtBoundary(edge) */&& (getMesh().edges().isBoundary(edge) || isConstrained(edge));
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
		return getMesh().edges().isLongestEdge(edge) && faceToQuality(edge) < Parameters.MIN_SPLIT_QUALITY;
	}

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
		if(allowVertexCollapse && isSlidePoint(vertex) && getMesh().vertices().degree(vertex) == 3) {
			/*Optional<E> toDeleteEdge = getMesh().streamEdges(vertex).filter(e -> !getMesh().isAtBoundary(e)).findAny();
			if(toDeleteEdge.isPresent()) {
				E edge = toDeleteEdge.get();
				E twin = getMesh().getTwin(edge);
				// 2 triangles!
				return edge.equals(getMesh().getNext(getMesh().getNext(getMesh().getNext(edge)))) && twin.equals(getMesh().getNext(getMesh().getNext(getMesh().getNext(twin))));
			} else {
				return false;
			}*/
			return true;
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
		double absForce = getAbsVelocity(vertex);
		double forceMax = edgeLengthFunc.apply(vertex) * Parameters.FSCALE * scalingFactor * 3;
		//TODO remove magic numbers
		boolean breaking = absForce > forceMax * 0.3 && force < forceMax * 0.1;
		/*if(breaking) {
			System.out.println("test");
		}*/
		return breaking;
		//return absForce > desiredLen && force / absForce < Parameters.MIN_FORCE_RATIO;
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
		if(isBoundary(vertex)) {

			// TODO: get rid of VPoint
			VPoint position = getMesh().vertices().toPoint(vertex);
			double distance = distanceFunc.apply(position);

			double x = position.getX();
			double y = position.getY();

			// the gradient (dx, dy)
			double dGradPX = (distanceFunc.apply(position.add(new VPoint(deps, 0))) - distance) / deps;
			double dGradPY = (distanceFunc.apply(position.add(new VPoint(0, deps))) - distance) / deps;

			// TODO: maybe a softer projection * deltaT
			//double scale = Math.abs(distance) > initialEdgeLen * 0.7 ? 0.7 : 1.0;
			double projX = dGradPX * distance;
			double projY = dGradPY * distance;

			double newX = x - projX;
			double newY = y - projY;

			// back projection towards the inside if the point is outside
			if(isOutside(distance)) {
				return new VPoint(newX, newY);
			}
			// back projection towards the inside if the point is inside (to improve the convergence rate of the algorithm)
			else if(isInsideProjectionValid(vertex, newX, newY)) {
				return new VPoint(newX, newY);
			}
		}

		return new VPoint(vertex.getX(), vertex.getY());
	}

	private boolean hasRefiner(){
		return refiner != null;
	}

	private void computeFixPoints() {
		if(hasRefiner()) {
			refiner.getFixPoints().forEach(v -> setFixPoint(v, true));
		}
	}

	/**
	 * This method can be used if the input for EikMesh is a valid triangulation and there is no
	 * distance function available. By default, the distance function is required to project points onto
	 * the boundary. We replace this requirement by projecting points onto the boundary segments i.e. points
	 * can move on the lines of the segment-bound or a hole of a PSLG, that is a triangulation. Therefore,
	 * this method constructs a map : boundary vertex -> line which gives quick access to the projection line.
	 */
	private void computeSlidingLines() {
		pointToSlidingLine = new HashMap<>();
		var vertices = getMesh().vertices();
		var edges = getMesh().edges();

		for (F boundaryFace : getMesh().faces().getBoundaryAndHoles()) {
			List<V> slicePoints = new ArrayList<>();
			Optional<V> optionalV = vertices.streamVerticesOf(boundaryFace).filter(v -> isFixPoint(v)).findFirst();

			if (optionalV.isPresent()) {
				V startFixPoint = optionalV.get();
				V sf = startFixPoint;

				E edge = edges.getBoundaryEdge(startFixPoint).get();
				var iterator = new EdgeIterator<>(getMesh(), edge);

				do {
					edge = iterator.next();
					V vertex = vertices.getEndOf(edge);

					if (isFixPoint(vertex) && !startFixPoint.equals(vertex)) {
						VLine line = new VLine(vertices.toPoint(startFixPoint), vertices.toPoint(vertex));
						for (V slicePoint : slicePoints) {
							pointToSlidingLine.put(slicePoint, line);
							//setFixPoint(slicePoint, true);
						}
						startFixPoint = vertex;
						slicePoints.clear();
					} else {
						slicePoints.add(vertex);
					}
				} while (iterator.hasNext());

				if (!startFixPoint.equals(sf)) {
					VLine line = new VLine(vertices.toPoint(startFixPoint), vertices.toPoint(sf));
					for (V slicePoint : slicePoints) {
						pointToSlidingLine.put(slicePoint, line);
						//setFixPoint(slicePoint, true);
					}
				}
			}
		}
		assert vertices.getBoundaryVertices().stream().filter(v -> isSlidePoint(v)).allMatch(v -> pointToSlidingLine.containsKey(v));
	}

	/**
	 * Tests if a point is outside which is determined by the <tt>distance</tt> value.
	 *
	 * @param distance
	 *
	 * @return true if the point is outside, false otherwise
	 */
	private boolean isOutside(@NotNull final double distance) {
		return distance > 0;
	}

	/**
	 * Tests if the inside projection is valid which is the case if the angle3D at the vertex (at the boundary)
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
		Optional<E> boundaryEdgeOpt = getMesh().edges().getBoundaryEdge(vertex);

		if(!boundaryEdgeOpt.isPresent()) {
			return false;
		}
		else {
			// TODO: if the algorithm runs in parallel this might lead to unexpected results!
			E boundaryEdge = boundaryEdgeOpt.get();
			VPoint p = getMesh().vertices().toPoint(vertex);
			VPoint q = getMesh().edges().endToPoint(getMesh().edges().getNext(boundaryEdge));
			VPoint r = getMesh().edges().endToPoint(getMesh().edges().getPrev(boundaryEdge));
			double angle = GeometryUtils.angle(r, p, q);
			return angle > Math.PI || (GeometryUtils.isLeftOf(r, p, newX, newY) && GeometryUtils.isLeftOf(p, q, newX, newY));
		}
	}

	private Set<IPoint> generateFixPoints() {
		List<VShape> shapes = new ArrayList<>(this.shapes.size()+1);
		shapes.addAll(this.shapes);

		Set<IPoint> ancherPoints = generateAnchorPoints(shapes);
		ancherPoints.addAll(fixPoints);
		return ancherPoints;
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
			streamEdges()
					.filter(e -> getTriangulation().getMeshBuilder().changeConnectivity().isIllegal(e))
					.filter(e -> !isConstrained(e))
					.forEach(e -> getTriangulation().getMeshBuilder().changeConnectivity().flipSync(e));
		}
		else {
			streamEdges()
					.filter(e -> getTriangulation().getMeshBuilder().changeConnectivity().isIllegal(e))
					.filter(e -> !isConstrained(e))
					.forEach(e -> getTriangulation().getMeshBuilder().changeConnectivity().flip(e));
		}
		return false;
	}

	/**
	 * Computation of the factor which transforms relative edge length into absolute ones.
	 */
	private double computeEdgeScalingFactor(@NotNull final IEdgeLengthFunction edgeLengthFunc) {
		double edgeLengthSum = streamEdges()
				.map(edge -> getMesh().edges().toLine(edge))
				.mapToDouble(line -> line.length())
				.sum();

		double desiredEdgeLenSum = streamEdges()
				.map(edge -> getMesh().edges().toLine(edge))
				.map(line -> line.midPoint())
				.mapToDouble(midPoint -> edgeLengthFunc.apply(midPoint)).sum();
		return Math.sqrt((edgeLengthSum * edgeLengthSum) / (desiredEdgeLenSum * desiredEdgeLenSum));
	}


	// helper methods
	private Stream<E> streamEdges() {
		return runParallel ? getMesh().edges().streamParallel() : getMesh().edges().stream();
	}

	private Stream<V> streamVertices() {
		return runParallel ? getMesh().vertices().streamParallel() : getMesh().vertices().stream();
	}

	/**
	 * Returns true if and only if the vertex {@link V} is a fix point.
	 *
	 * @param vertex the vertex of interest
	 * @return true if and only if the vertex {@link V} is a fix point.
	 */
	private boolean isFixedVertex(final V vertex) {
		return isFixPoint(vertex) /*|| nonEmptyBaseMode &&getMesh().isAtBoundary(vertex)*/;
	}

	/**
	 * Returns the force which is currently i.e. which was computed by
	 * {@link GenEikMesh#computeForce(IVertex)} applied to the vertex.
	 *
	 * @param vertex the vertex of interest
	 * @return the force which is currently applied to the vertex
	 */
	private IPoint getForce(final V vertex) {
		return getVelocity(vertex);
	}

	/**
	 * Applies the force of the vertex to the vertex which results
	 * in an displacement of the vertex.
	 *
	 * @param vertex the vertex of interest
	 */
	private void applyForce(final V vertex) {
		IPoint velocity = getForce(vertex);
		double factor = 1.0;
		IPoint movement = velocity.scalarMultiply(delta * factor);
		int count = 0;
		while(!move(vertex, vertex.getX() + movement.getX(), vertex.getY() + movement.getY()) && count < 10) {
			factor /= 2.0;
			movement = velocity.scalarMultiply(delta * factor);
			count++;
		}
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
	private Set<IPoint> generateAnchorPoints(@NotNull final Collection<? extends  VShape> shapes) {
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
		Predicate<F> isOutside = f -> distanceFunc.apply(getMesh().faces().toMidpoint(f)) > 0;
		Predicate<F> isSeparated = f -> getMesh().faces().isSeparated(f);
		//Predicate<F> isInvalid = f -> !getTriangulation().isValid(f);
		Predicate<F> isOfLowQuality = f -> faceToQuality(f) < Parameters.MIN_TRIANGLE_QUALITY && !isShortBoundaryEdge(f);
		Predicate<F> isBoundary = f -> getMesh().faces().isBoundary(f);

		Predicate<F> mergePredicate = isSeparated/*.or(isInvalid)*/.or(isOutside).or(isOfLowQuality);
		try {
			getTriangulation().getMeshBuilder().changeConnectivity().removeFacesAtBoundary(mergePredicate, isBoundary);
		} catch (IllegalMeshException e) {
			log.error("error!");
		}

		if(useSlidingLines) {
			updateProjectionLines();
		}
	}

	private void updateProjectionLines() {
		// this is a bad code which just updates the pointToSlidingLine requiring to run on the border possibly in both directions
		var vertices = getMesh().vertices();

		vertices.stream().filter(v -> isSlidePoint(v)).filter(v -> !pointToSlidingLine.containsKey(v)).forEach(v -> {
			E edge = getMesh().edges().getBoundaryEdge(v).get();
			V v1 = vertices.getEndOf(getMesh().edges().getNext(edge));
			V v2 = vertices.getEndOf(getMesh().edges().getPrev(edge));

			if(isFixPoint(v1) && isFixPoint(v2)) {
				pointToSlidingLine.put(v, new VLine(vertices.toPoint(v1), vertices.toPoint(v2)));
			} else {
				EdgeIterator<V, E, F> edgeIterator = new EdgeIterator<>(getMesh(), edge);
				while (edgeIterator.hasNext()) {
					E next = edgeIterator.next();
					V vertex = vertices.getEndOf(next);
					if(pointToSlidingLine.containsKey(vertex)) {
						pointToSlidingLine.put(v, pointToSlidingLine.get(vertex));
					}

					if(isFixPoint(vertex)) {
						break;
					}
				}

				if(!pointToSlidingLine.containsKey(v)) {
					EdgeIteratorReverse<V, E, F> reverseEdgeIterator = new EdgeIteratorReverse<>(getMesh(), edge);
					while (reverseEdgeIterator.hasNext()) {
						E next = reverseEdgeIterator.next();
						V vertex = vertices.getEndOf(next);
						if(pointToSlidingLine.containsKey(vertex)) {
							pointToSlidingLine.put(v, pointToSlidingLine.get(vertex));
						}

						if(isFixPoint(vertex)) {
							break;
						}
					}
				}
			}
		});
	}

	/**
	 * <p>Shrinks the boundary such that there are no more triangles outside the boundary i.e. where the distance is positive.</p>
	 */
	private void shrinkBoundary() {
		Predicate<F> removePredicate = face -> distanceFunc.apply(getMesh().faces().toMidpoint(face)) > 0;
		getMeshBuilder().changeConnectivity().shrinkBoundary(removePredicate, true);
	}

	/**
	 * <p>Shrinks the border such that there are no more triangles outside the boundary i.e. where the distance is positive.
	 * Note the border is part of the whole boundary which is defined by the border and the holes.</p>
	 */
	private void shrinkBorder() {
		Predicate<F> removePredicate = face -> distanceFunc.apply(getMesh().faces().toMidpoint(face)) > 0;
		getMeshBuilder().changeConnectivity().shrinkBorder(removePredicate, true);
	}

	/**
	 * <p>Creates holes everywhere where the distance function is positive. Neighbouring holes will be merged.</p>
	 */
	private void createHoles() {
		List<F> faces = getMesh().faces().getAll();
		for(F face : faces) {
			if(!getMesh().faces().isDestroyed(face) && !getMesh().faces().isHole(face)) {
				getMeshBuilder().changeConnectivity().createHole(face, f -> distanceFunc.apply(getMesh().faces().toMidpoint(f)) > 0, true);
			}
		}
	}


	private boolean isShortBoundaryEdge(@NotNull final F face) {
		E edge = getMesh().edges().getBoundaryEdge(face).get();
		// corner => can be deleted

		VLine l1 = getMesh().edges().toLine(edge);
		VLine l2 = getMesh().edges().toLine(getMesh().edges().getNext(edge));
		VLine l3 = getMesh().edges().toLine(getMesh().edges().getPrev(edge));

		if(l1.length() < l2.length() || l1.length() < l3.length()) {
			return true;
		}

		return false;
	}

	public void setDistanceFunc(@NotNull final IDistanceFunction distanceFunc) {
		this.distanceFunc = distanceFunc;
		reset();
		retriangulate();
	}

	/*private boolean isFrozen(@NotNull final V vertex) {
		if(isFixPoint(vertex)) {
			return true;
		}
		return vertex.getMovement() / initialEdgeLen < Parameters.DPTOL;
	}*/

	/*	public void setFixPointRelation(@NotNull final Map<V, VPoint> fixPointRelation) {
		this.fixPointRelation = fixPointRelation;
	}*/


	/*private void splitTriangles() {
		if(poorFaces.isEmpty()) {
			poorFaces = getMesh().streamFaces().filter(f -> faceToQuality(f) < 0.2).limit(10).collect(Collectors.toCollection(LinkedList::new));
		}

		while (!poorFaces.isEmpty()) {
			F f = poorFaces.poll();
			if(faceToQuality(f) < 0.2) {
				VTriangle triangle = getMesh().toTriangle(f);
				VPoint c = triangle.getCircumcenter();
				getTriangulation().insertVertex(c.getX(), c.getY());
			}
		}
    }*/

    /*private void clearPoorTriangles() {
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
    }*/

	private void retriangulate() {
		synchronized (getMeshBuilder()) {
			log.info("EikMesh re-triangulates in step " + nSteps);
			getTriangulation().recompute();
			shrinkBorder();
			createHoles();
			//removeTrianglesOutsideBBox();
			//removeTrianglesInsideObstacles();
			/*try {
				removeFacesOutside(distanceFunc);
			} catch (IllegalMeshException e) {
				log.error("error!");
			}*/
		}
	}

	private boolean isBoundary(@NotNull final V vertex) {
		return getMesh().edges().isBoundary(getMesh().edges().getOf(vertex));
	}

	/*
	 * The following methods are helper methods to quickly access properties saved on vertices, edges and faces
	 */

	private void setConstraint(E edge, boolean constraint) {
		constraintC.setValue(edge, constraint);
	}

	private boolean isConstrained(E edge) {
		return constraintC.getValue(edge);
	}

	private void setFixPoint(V vertex, boolean fixPoint) {
		fixpointC.setValue(vertex, fixPoint);
	}

	public boolean isFixPoint(V vertex) {
		return fixpointC.getValue(vertex);
	}

	private void setVelocity(V vertex, IPoint velocity) {
		velocityXC.setValue(vertex, velocity.getX());
		velocityYC.setValue(vertex, velocity.getY());
	}

	private void setVelocityX(V vertex, double velX) {
		velocityXC.setValue(vertex, velX);
	}

	private void setVelocityY(V vertex, double velY) {
		velocityYC.setValue(vertex, velY);
	}

	private void increaseVelocity(V vertex, IPoint dvelocity) {
		increaseVelocityX(vertex, dvelocity.getX());
		increaseVelocityY(vertex, dvelocity.getY());
	}

	private double getVelocityX(V vertex) {
		return velocityXC.getValue(vertex);
	}

	private void increaseVelocityX(V vertex, double dVelX) {
		double velX = velocityXC.getValue(vertex);
		velocityXC.setValue(vertex, velX + dVelX);
	}

	private void increaseVelocityY(V vertex, double dVelY) {
		double velY = velocityYC.getValue(vertex);
		velocityYC.setValue(vertex, velY + dVelY);
	}

	private double getVelocityY(V vertex) {
		return velocityYC.getValue(vertex);
	}

	private VPoint getVelocity(V vertex) {
		return new VPoint(getVelocityX(vertex), getVelocityY(vertex));
	}

	private void setAbsVelocity(V vertex, double absVelocity) {
		absVelocityC.setValue(vertex, absVelocity);
	}

	private void increaseAbsVelocity(V vertex, double dAbsVelocity) {
		double absVel = absVelocityC.getValue(vertex);
		absVelocityC.setValue(vertex, absVel + dAbsVelocity);
	}

	// setter to configure the algorithm strategy.
	private double getAbsVelocity(V vertex) {
		return absVelocityC.getValue(vertex);
	}

	public void setAllowEdgeSplits(final boolean allowEdgeSplits) {
		this.allowEdgeSplits = allowEdgeSplits;
	}

	public void setAllowVertexCollapse(final boolean allowVertexCollapse) {
		this.allowVertexCollapse = allowVertexCollapse;
	}

	public void setUseVirtualEdges(final boolean useVirtualEdges) {
		this.useVirtualEdges = useVirtualEdges;
	}

	public void setRemoveLowBoundaryTriangles(final boolean removeLowBoundaryTriangles) {
		this.removeLowBoundaryTriangles = removeLowBoundaryTriangles;
	}

	public void setEdgeLenFunction(@NotNull final IEdgeLengthFunction edgeLengthFunc) {
		this.edgeLengthFunc = edgeLengthFunc;
		reset();
	}

	/**
	 * Unused old strategy to deal with very obtuse triangles at the boundary.
	 * This is replaced by the concept of virtual edges.
	 *
	 * @param edge
	 */
	private void computeBoundaryForces(final E edge) {
		/*
		 * EikMesh improvements
		 */
		var edges = getMesh().edges();
		var vertices = getMesh().vertices();
		if(edges.isBoundary(edge)) {
			E twin = edges.getTwin(edge);
			E next = edges.getNext(twin);
			E prev = edges.getPrev(twin);

			if((edges.toLine(next).length() + edges.toLine(prev).length()) * 1.1 < edges.toLine(edge).length()) {
				// get the opposite point

				V v1 = vertices.getEndOf(edge);
				V v2 = vertices.getEndOf(edges.getPrev(edge));
				V v3 = vertices.getEndOf(edges.getNext(edges.getTwin(edge)));

				// TODO: Get rid of VPoint
				VPoint midPoint = new VPoint((v1.getX() + v2.getX()) * 0.5, (v1.getY() + v2.getY()) * 0.5);
				VPoint midLeft = new VPoint((v1.getX() + v3.getX()) * 0.5, (v1.getY() + v3.getY()) * 0.5);
				VPoint midRight = new VPoint((v2.getX() + v3.getX()) * 0.5, (v2.getY() + v3.getY()) * 0.5);
				VPoint q = new VPoint((midPoint.getX() + v3.getX()) * 0.5, (midPoint.getY() + v3.getY()) * 0.5);

				double desiredLen = edgeLengthFunc.apply(q) * Parameters.FSCALE * scalingFactor;
				double len = new VLine(midPoint, vertices.toPoint(v3)).length();

				double cDes = edgeLengthFunc.apply(midLeft);
				double aDes = edgeLengthFunc.apply(new VLine(midLeft, edges.endToPoint(edge)).midPoint());
				double bDes = Math.sqrt(cDes*cDes - aDes*aDes) * Parameters.FSCALE * scalingFactor;

				double c = edges.toLine(edges.getPrev(twin)).length();
				double a = edges.toLine(edge).length() * 0.5;
				double b = Math.sqrt(c*c - a*a);


				double lenDiff = desiredLen - len;

				if(lenDiff < 0 /*&& lenDiff > -desiredLen*/) {
					lenDiff *= 0.1;
				}

				// TODO: get rid of VPoint
				double forceX = (midLeft.getX() - v3.getX()) * (lenDiff / len);
				double forceY = (midLeft.getY() - v3.getY()) * (lenDiff / len);
				double forceLen = GeometryUtils.length(forceX, forceY);
				increaseVelocityX(v3, forceX);
				increaseVelocityY(v3, forceY);
				increaseAbsVelocity(v3, forceLen);
			}
		}
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
	}

	private boolean isNotColinear (@NotNull final E edge) {
		V p1 = getMesh().getVertex(getMesh().getPrev(edge));
		V p2 = getMesh().getVertex(edge);
		V p3 = getMesh().getVertex(getMesh().getNext(edge));

		// not co-linear?
		return Math.abs(GeometryUtils.ccw(p1, p2, p3)) > GeometryUtils.DOUBLE_EPS;
	}

	*/
}