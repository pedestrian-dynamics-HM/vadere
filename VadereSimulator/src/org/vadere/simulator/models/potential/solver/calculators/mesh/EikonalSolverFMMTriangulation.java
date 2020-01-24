package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.math.MathUtil;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * This class computes the traveling time T using the fast marching method for arbitrary triangulated meshes.
 * The quality of the result depends on the quality of the triangulation. For a high accuracy the triangulation
 * should not contain too many non-acute triangles.
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 */
public class EikonalSolverFMMTriangulation<V extends IVertex, E extends IHalfEdge, F extends IFace> implements EikonalSolver {

	private static Logger logger = Logger.getLogger(EikonalSolverFMMTriangulation.class);

	public static final String namePotential = "potential";
	public static final String namePathFindingTag = "pathFindingTag";
	public static final String nameNonAccuteFace = "nonAccuteFace";

	private final String identifier;

	static {
		logger.setInfo();
	}

	/**
	 * The time cost function defined on the geometry.
	 */
	private ITimeCostFunction timeCostFunction;

	/**
	 * Gives the distance to the boundary i.e. the targets
	 */
	private IDistanceFunction distFunc;

	private Collection<V> targetVertices;

	/**
	 * The triangulation the solver uses.
	 */
	private IIncrementalTriangulation<V, E, F> triangulation;

	/**
	 * Indicates that the computation of T has been completed.
	 */
	private boolean calculationFinished;

	/**
	 * The narrow-band of the fast marching method.
	 */
	private PriorityQueue<V> narrowBand;

	/**
	 * Comparator for the heap. Vertices of points with small potentials are at the top of the heap.
	 */
	private Comparator<V> pointComparator = (v1, v2) -> {
		if (getPotential(v1) < getPotential(v2)) {
			return -1;
		} else if(getPotential(v1) > getPotential(v2)) {
			return 1;
		}
		else {
			return 0;
		}
	};

	// Note: The order of arguments in the constructors are exactly as they are since the generic type of a collection is only known at run-time!

	/**
	 * Constructor for certain target points.
	 * @param identifier        a unique identifier (required if the underlying mesh saves more than 1 potential)
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param targetPoints      Points where the propagating wave starts i.e. points that are part of the target area.
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public EikonalSolverFMMTriangulation(@NotNull final String identifier,
	                                     @NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final Collection<IPoint> targetPoints,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		this.identifier = identifier;
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.narrowBand = new PriorityQueue<>(pointComparator);
		this.targetVertices = new HashSet<>();
		this.distFunc = p -> IDistanceFunction.createToTargetPoints(targetPoints).apply(p);

		for(IPoint point : targetPoints) {
			F face = triangulation.locateFace(point).get();
			assert !getMesh().isBoundary(face);

			if(!getMesh().isBoundary(face)) {
				targetVertices.addAll(getMesh().getVertices(face));
				for(F neighbourFace : getMesh().getFaceIt(face)) {
					if(!getMesh().isBoundary(neighbourFace)) {
						targetVertices.addAll(getMesh().getVertices(neighbourFace));
					}
				}
			}
			//initialFace(face, p -> point.distance(p));
		}
	}

	public EikonalSolverFMMTriangulation(@NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final Collection<IPoint> targetPoints,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		this("", timeCostFunction, targetPoints, triangulation);
	}

	/**
	 * Constructor for certain target points.
	 *
	 * @param identifier        a unique identifier (required if the underlying mesh saves more than 1 potential)
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 * @param targetVertices    Points where the propagating wave starts i.e. points that are part of the target area.
	 */
	public EikonalSolverFMMTriangulation(@NotNull final String identifier,
										 @NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                                     @NotNull final Collection<V> targetVertices
	) {
		this.identifier = identifier;
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.narrowBand = new PriorityQueue<>(pointComparator);
		this.targetVertices = new HashSet<>();
		this.distFunc = p -> IDistanceFunction.createToTargetPoints(targetVertices).apply(p);

		for(V vertex : targetVertices) {
			this.targetVertices.add(vertex);
			for(V neighbouringVertices : getMesh().getAdjacentVertexIt(vertex)) {
				this.targetVertices.add(neighbouringVertices);
			}
		}
	}

	public EikonalSolverFMMTriangulation(@NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                                     @NotNull final Collection<V> targetVertices
	) {
		this("", timeCostFunction, triangulation, targetVertices);
	}

	/**
	 * Constructor for certain target shapes.
	 *
	 * @param identifier        a unique identifier (required if the underlying mesh saves more than 1 potential).
	 * @param targetShapes      shapes that define the whole target area.
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public EikonalSolverFMMTriangulation(@NotNull final String identifier,
										 @NotNull final Collection<VShape> targetShapes,
	                                     @NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		this.identifier = identifier;
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.narrowBand = new PriorityQueue<>(pointComparator);
		this.targetVertices = new HashSet<>();
		this.distFunc = IDistanceFunction.createToTargets(targetShapes);

		for(VShape shape : targetShapes) {
			getMesh().streamFaces()
					.filter(f -> shape.intersects(getMesh().toTriangle(f)))
					.forEach(f -> targetVertices.addAll(getMesh().getVertices(f)));
		}
	}

	public EikonalSolverFMMTriangulation(@NotNull final Collection<VShape> targetShapes,
	                                     @NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		this("", targetShapes, timeCostFunction, triangulation);
	}

	/**
	 * Constructor for certain vertices of the triangulation.
	 *
	 * @param identifier        a unique identifier (required if the underlying mesh saves more than 1 potential).
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 * @param targetVertices    vertices which are part of the triangulation where the propagating wave starts i.e. points that are part of the target area.
	 * @param distFunc          the distance function (distance to the target) which is negative inside and positive outside the area of interest
	 */
	public EikonalSolverFMMTriangulation(@NotNull final String identifier,
										 @NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                                     @NotNull final Collection<V> targetVertices,
	                                     @NotNull final IDistanceFunction distFunc
	) {
		this.identifier = identifier;
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.narrowBand = new PriorityQueue<>(pointComparator);
		this.distFunc = distFunc;
		this.targetVertices = targetVertices;

		for(F face : triangulation.getMesh().getFaces()) {
			if(isNonAcute(triangulation.getMesh().getEdge(face))) {
				setNoneAccuteFace(face);
			}
		}
	}

	public EikonalSolverFMMTriangulation(@NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                                     @NotNull final Collection<V> targetVertices,
	                                     @NotNull final IDistanceFunction distFunc
	) {
		this("", timeCostFunction, triangulation, targetVertices, distFunc);
	}

	private void setNoneAccuteFace(@NotNull final F face) {
		getMesh().setBooleanData(face, nameNonAccuteFace, true);
	}

	/**
	 * Computes and sets the potential of all points of a face based on the distance function.
	 *
	 * @param face              the face
	 * @param distanceFunction  the distance function
	 */
	private void initialFace(@NotNull final F face, @NotNull final IDistanceFunction distanceFunction) {
		for(V vertex : getMesh().getVertexIt(face)) {
			double distance = distanceFunction.apply(vertex);
			if(getPathFindingTag(vertex) != PathFindingTag.Undefined) {
				narrowBand.remove(vertex);
			}
			updatePotential(vertex, distance / getTimeCost(vertex));
			setPathFindingTag(vertex, PathFindingTag.Reached);
			narrowBand.add(vertex);
		}
	}

	/**
	 * Calculate the fast marching solution. This is called only once,
	 * subsequent calls only return the result of the first.
	 */
	@Override
	public void initialize() {
		reset();
		triangulation.enableCache();
		if (!calculationFinished) {
			while (narrowBand.size() > 0) {
				V vertex = narrowBand.poll();
				setPathFindingTag(vertex, PathFindingTag.Reached);
				updatePotentialOfNeighbours(vertex);
			}
			calculationFinished = true;
		}
	}

	private void reset() {
		triangulation.getMesh().streamVertices().forEach(v -> setPathFindingTag(v, PathFindingTag.Undefined));
		triangulation.getMesh().streamVertices().forEach(v -> setPotential(v, Double.MAX_VALUE));
		calculationFinished = false;

		for(V vertex : targetVertices) {
			double distance = Math.max(distFunc.apply(vertex), 0);

			if(getPathFindingTag(vertex) != PathFindingTag.Undefined) {
				narrowBand.remove(vertex);
			}
			updatePotential(vertex, distance / getTimeCost(vertex));
			setPathFindingTag(vertex, PathFindingTag.Reached);
			narrowBand.add(vertex);

			for(V v : triangulation.getMesh().getAdjacentVertexIt(vertex)) {
				if(getPathFindingTag(v)  == PathFindingTag.Undefined) {
					double dist = Math.max(distFunc.apply(v), 0);
					logger.debug("T at " + v + " = " + dist);
					updatePotential(v, dist / getTimeCost(v));
					setPathFindingTag(v, PathFindingTag.Reachable);
					narrowBand.add(v);
				}
			}
		}
	}

	private double getTimeCost(@NotNull final V vertex) {
		return timeCostFunction.costAt(vertex);
	}

	private void updatePotential(@NotNull final V vertex, final double potential) {
		setPotential(vertex, Math.min(getPotential(vertex), potential));
	}

	// unknownPenalty is ignored.
	@Override
	public double getPotential(@NotNull final IPoint pos, final double unknownPenalty, final double weight) {
		return weight * getPotential(pos.getX(), pos.getY());
	}

	@Override
	public double getPotential(IPoint pos, double unknownPenalty, double weight, final Object caller) {
		return weight * getPotential(pos.getX(), pos.getY(), caller);
	}

	@Override
	public Function<IPoint, Double> getPotentialField() {
		IIncrementalTriangulation<V, E, F> clone = triangulation.clone();
		return p -> getPotential(clone, p.getX(), p.getY());
	}

	@Override
	public double getPotential(final double x, final double y) {
		return getPotential(triangulation, x, y);
	}

	@Override
	public double getPotential(double x, double y, final Object caller) {
		return getPotential(triangulation, x, y, caller);
	}

	@Override
	public IMesh<?, ?, ?> getDiscretization() {
		return triangulation.getMesh().clone();
	}

	/**
	 * Returns the interpolated value of the traveling time T at (x, y) for a triangulation on which the
	 * eikonal equation was solved.
	 *
	 * @param triangulation the triangulation for which the triangulation was solved
	 * @param x             the x-coordinate of the point
	 * @param y             the y-coordinate of the point
	 *
	 *
	 * @return the interpolated value of the traveling time T at (x, y)
	 */
	private double getPotential(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			final double x,
			final double y,
			@Nullable final Object caller) {

		Optional<F> optFace;
		if(caller != null) {
			optFace = triangulation.locateFace(new VPoint(x, y), caller);
		} else {
			optFace = triangulation.locateFace(new VPoint(x, y));
		}

		double result = Double.MAX_VALUE;
		if(!optFace.isPresent()) {
			//logger.warn("no face found for coordinates (" + x + "," + y + ")");
		}
		else if(optFace.isPresent() && triangulation.getMesh().isBoundary(optFace.get())) {
			//	logger.warn("no triangle found for coordinates (" + x + "," + y + ")");
		}
		else {
			E edge = triangulation.getMesh().getEdge(optFace.get());
			V v1 = triangulation.getMesh().getVertex(edge);
			V v2 = triangulation.getMesh().getVertex(triangulation.getMesh().getNext(edge));
			V v3 = triangulation.getMesh().getVertex(triangulation.getMesh().getPrev(edge));
			result = InterpolationUtil.barycentricInterpolation(v1, v2, v3, v -> getPotential(v), x, y);
		}
		return result;
	}

	private double getPotential(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			final double x,
			final double y) {
		return getPotential(triangulation, x, y, null);
	}

	private IMesh<V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	/**
	 * Updates the the traveling times T of all neighbours of <tt>vertex</tt>.
	 *
	 * @param vertex the vertex
	 */
	private void updatePotentialOfNeighbours(@NotNull final V vertex) {
		for(V neighbour : getMesh().getAdjacentVertexIt(vertex)) {
			updatePotential(neighbour);
		}
	}

	/**
	 * Updates the traveling time T of a certain vertex by recomputing it and
	 * updates the narrow band if necessary. If the recomputed value is larger
	 * than the old value, nothing will change.
	 *
	 * @param vertex the vertex for which T will be updated
	 */
	private void updatePotential(@NotNull final V vertex) {
		double potential = recomputePotential(vertex);
		if(potential < getPotential(vertex)) {
			if(getPathFindingTag(vertex) == PathFindingTag.Reachable) {
				narrowBand.remove(vertex);
			}
			setPotential(vertex, potential);
			setPathFindingTag(vertex, PathFindingTag.Reachable);
			narrowBand.add(vertex);
		}

		if(getPathFindingTag(vertex) == PathFindingTag.Undefined) {
			logger.debug("could not set neighbour vertex" + vertex);
		}
	}

	/**
	 * Recomputes traveling time T of a potential point of a certain vertex by
	 * computing all possible traveling times for each neighbouring points
	 * returning the minimum.
	 *
	 * @param vertex the vertex which represents the potential point.
	 *
	 * @return the recomputed potential of a potential point
	 */
	private double recomputePotential(@NotNull final V vertex) {
		// loop over all, check whether the point is contained and update its
		// value accordingly
		double potential = Double.MAX_VALUE;

		for(E edge : getMesh().getEdgeIt(vertex)) {
			if(!getMesh().isBoundary(edge)) {
				potential = Math.min(computePotential(edge), potential);
			}
		}
		return potential;
	}


	/**
	 * Tests whether the triangle / face of <tt>edge</tt> is non-acute. In this case we can not use
	 * the triangle for computation but have to search for numerical support.
	 *
	 * @param edge  the edge defining the face / triangle
	 *
	 * @return true if the face / triangle is non-acute, false otherwise
	 */
	private boolean isNonAcute(@NotNull final E edge) {
		VPoint p1 = getMesh().toPoint(getMesh().getPrev(edge));
		VPoint p2 = getMesh().toPoint(edge);
		VPoint p3 = getMesh().toPoint(getMesh().getNext(edge));

		double angle1 = GeometryUtils.angle(p1, p2, p3);

		// non-acute triangle
		double rightAngle = Math.PI/2;
		return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
	}

	private boolean isNonAcute(@NotNull final F face) {
		return getMesh().getBooleanData(face, nameNonAccuteFace);
	}

	/**
	 * Updates a point given a triangle. The point can only be updated if the
	 * triangle triangleContains it and the other two points are in the frozen band.
	 *
	 * @param point a point for which the potential should be re-computed
	 * @param face  a face neighbouring the point
	 */

	/**
	 * Updates a point (the point where the edge ends) given a triangle (which is the face of the edge).
	 * The point can only be updated if the triangle triangleContains it and the other two points are in the frozen band.
	 *
	 * @param edge the edge defining the point and the triangle
	 * @return the recomputed potential
	 */
	private double computePotential(@NotNull final E edge) {
		E e1 = getMesh().getNext(edge);
		E e2 = getMesh().getPrev(edge);
		F face = getMesh().getFace(edge);

		if(isNonAcute(face)) {
			E prev = getMesh().getPrev(edge);
			V v = getMesh().getVertex(edge);
			return computeVirtualPotential(prev, v);

		} else {
			if(isFeasibleForComputation(getMesh().getVertex(e1)) && isFeasibleForComputation(getMesh().getVertex(e2))) {
				//if(!nonAccuteTris.contains(face)) {
				//double potential = computePotential(point, p1, p2);
				//logger.info("compute potential " + potential);
				return computePotential(edge, e1, e2);
				//} // we only try to find a virtual vertex if both points are already frozen
            /*else {
                logger.debug("special case for non-acute triangle");
                Optional<P> optPoint = walkToNumericalSupport(halfEdge, face);

                if(optPoint.isPresent()) {
                    P surrogatePoint = optPoint.get();
                    if(isFeasibleForComputation(surrogatePoint)) {
                        //logger.info("feasible point found for " + point + " and " + face);
                        return Math.min(computePotential(point, surrogatePoint, p2), computePotential(point, p1, surrogatePoint));
                    }
                    else {
                        logger.warn("no feasible point found for " + point + " and " + face);
                    }
                }
                else {
                    logger.warn("no point found for " + point + " and " + face);
                }
            }*/
			}
		}

		return Double.MAX_VALUE;
	}

	private double computeVirtualPotential(@NotNull final E edge, @NotNull final V v3){
		if(getMesh().isAtBoundary(edge)) {
			return Double.MAX_VALUE;
		}

		V v1 = getMesh().getVertex(edge);
		V v2 = getMesh().getVertex(getMesh().getPrev(edge));

		double potential = Double.MAX_VALUE;
		if(isNonAcute(v1, v2, v3)) {
			if(isFeasibleForComputation(v1) && isFeasibleForComputation(v3)) {
				potential = computePotential(v3, v1, v2);
			}
		} else {
			potential = Math.min(computeVirtualPotential(getMesh().getNext(getMesh().getTwin(edge)), v3),  computeVirtualPotential(getMesh().getPrev(getMesh().getTwin(edge)), v3));
		}

		return potential;
	}

	private boolean isNonAcute(V v1, V v2, V v3) {
		double angle1 = GeometryUtils.angle(v1, v2, v3);

		// non-acute triangle
		double rightAngle = Math.PI/2;
		return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
	}

	/**
	 * Defines the porperties for which a point is used to compute the traveling time of neighbouring points.
	 *
	 * @param v the point which is tested
	 *
	 * @return true if the point can be used for computation, false otherwise
	 */
	private boolean isFeasibleForComputation(final V v){
		//return p.getPathFindingTag().frozen;
		return getPathFindingTag(v) == PathFindingTag.Reachable || getPathFindingTag(v) == PathFindingTag.Reached;
	}

	/**
	 * In case of a non-acute triangle / face this method searches for numerical support. The point which offers this support
	 * has to lie inside a certain cone which starts at the vertex of <tt>halfedge</tt> and which contains only points p such
	 * that p and the vertex of <tt>halfedge</tt> and each of the other points of <tt>face</tt> form a acute and therefore
	 * valid triangle.
	 *
	 * @param halfEdge  the half-edge of the vertex for which a numerical support will be searched
	 * @param face      the face containing the <tt>halfedge</tt> for which a numerical support will be searched
	 *
	 * @return (optional) a numerical support such that a acute triangle can be formed or empty if no support can be found
	 */
	private Optional<V> walkToNumericalSupport(@NotNull final E halfEdge, @NotNull final F face) {
		assert getMesh().toTriangle(face).isNonAcute();

		E next = getMesh().getNext(halfEdge);
		E prev = getMesh().getPrev(halfEdge);

		VPoint p = new VPoint(getMesh().getVertex(halfEdge));
		VPoint pNext = new VPoint(getMesh().getVertex(next));
		VPoint pPrev = new VPoint(getMesh().getVertex(prev));

		VPoint direction1 = pNext.subtract(p).rotate(+Math.PI/2);
		VPoint direction2 = pPrev.subtract(p).rotate(-Math.PI/2);

		Predicate<E> isPointInCone = e ->
		{
			VPoint point = getMesh().toPoint(e);
			return  GeometryUtils.isLeftOf(p, p.add(direction2), point) &&
					GeometryUtils.isRightOf(p, p.add(direction1), point);
		};

		Predicate<E> isEdgeInCone = e -> isPointInCone.test(e) || isPointInCone.test(getMesh().getPrev(e));

		LinkedList<E> visitedFaces = triangulation.straightWalk2DGatherDirectional(face, direction2, isEdgeInCone);
		F destination = triangulation.getMesh().getFace(visitedFaces.getLast());

		assert !destination.equals(face);

		if(!getMesh().isBoundary(destination)) {

	        /*System.out.println(isPointInCone.test(visitedFaces.getLast()));

	        SimpleTriCanvas canvas = SimpleTriCanvas.simpleCanvas(getMesh());
	        visitedFaces.stream().map(e -> triangulation.getMesh().getFace(e)).forEach(f -> canvas.getColorFunctions().overwriteFillColor(f, Color.MAGENTA));
	        DebugGui.setDebugOn(true);
	        if(DebugGui.isDebugOn()) {
		        // attention the view is mirrowed.
		        canvas.addGuiDecorator(graphics -> {
			        Graphics2D graphics2D = (Graphics2D)graphics;
			        graphics2D.setColor(Color.GREEN);
			        graphics2D.setStroke(new BasicStroke(0.05f));
			        logger.info("p: " + p);
			        graphics2D.draw(new VLine(p, p.add(direction1.scalarMultiply(10))));
			        graphics2D.setColor(Color.BLUE);
			        graphics2D.draw(new VLine(p, p.add(direction2.scalarMultiply(10))));
			        //graphics2D.fill(new VCircle(new VPoint(getMesh().toPoint(startVertex)), 0.05));
			        //graphics2D.fill(new VCircle(q, 0.05));
		        });
		        DebugGui.showAndWait(canvas);
	        }*/
			/**
			 * find the support inside the face in O(3).
			 */
			return getMesh().streamEdges(destination).filter(e -> isPointInCone.test(e)).map(v -> getMesh().getVertex(v)).findAny();
		}
		else {
			logger.warn("walked to boundary");

	      /*  visitedFaces = triangulation.straightWalk2DGatherDirectional(face, direction2, isEdgeInCone);
            SimpleTriCanvas canvas = SimpleTriCanvas.simpleCanvas(getMesh());
	    visitedFaces.stream().map(e -> triangulation.getMesh().getFace(e)).forEach(f -> canvas.getColorFunctions().overwriteFillColor(f, Color.MAGENTA));
	    DebugGui.setDebugOn(true);
	    if(DebugGui.isDebugOn()) {
	    	// attention the view is mirrowed.
		    canvas.addGuiDecorator(graphics -> {
			    Graphics2D graphics2D = (Graphics2D)graphics;
			    graphics2D.setColor(Color.GREEN);
			    graphics2D.setStroke(new BasicStroke(0.05f));
			    logger.info("p: " + p);
			    graphics2D.draw(new VLine(p, p.add(direction1.scalarMultiply(10))));
			    graphics2D.setColor(Color.BLUE);
			    graphics2D.draw(new VLine(p, p.add(direction2.scalarMultiply(10))));
			    //graphics2D.fill(new VCircle(new VPoint(getMesh().toPoint(startVertex)), 0.05));
			    //graphics2D.fill(new VCircle(q, 0.05));
		    });
		    DebugGui.showAndWait(canvas);
	    }*/
			return Optional.empty();
		}
	}

	private PathFindingTag getPathFindingTag(@NotNull final V vertex) {
		return triangulation.getMesh().getData(vertex, identifier + "_" + namePathFindingTag, PathFindingTag.class).get();
	}

	private void setPathFindingTag(@NotNull final V vertex, final PathFindingTag tag) {
		triangulation.getMesh().setData(vertex, identifier + "_" + namePathFindingTag, tag);
	}

	private double getPotential(@NotNull final V vertex) {
		return triangulation.getMesh().getDoubleData(vertex, identifier + "_" + namePotential);
	}

	private void setPotential(@NotNull final V vertex, final double potential) {
		triangulation.getMesh().setDoubleData(vertex, identifier + "_" + namePotential, potential);
	}

	/**
	 * Computes the traveling time T at <tt>point</tt> by using the neighbouring points <tt>point1</tt> and <tt>point2</tt>.
	 *
	 * @param edge     the edge / point for which the traveling time is computed
	 * @param edge1    one neighbouring edge
	 * @param edge2    another neighbouring edge
	 *
	 * @return the traveling time T at <tt>edge</tt> by using the triangle (edge, edge1, edge2) for the computation
	 */
	private double computePotential(final E edge, final E edge1, final E edge2) {

		V point = getMesh().getVertex(edge);
		V point1 = getMesh().getVertex(edge1);
		V point2 = getMesh().getVertex(edge2);

		// see: Sethian, Level Set Methods and Fast Marching Methods, page 124.

		E e1; // A
		E e2; // B
		V p1;   // A
		V p2;   // B

		// assuming T(B) > T(A)
		if(getPotential(point1) > getPotential(point2)) {
			p2 = point1;
			e2 = edge1;
			p1 = point2;
			e1 = edge2;
		}
		else {
			p2 = point2;
			e2 = edge2;
			p1 = point1;
			e1 = edge1;
		}

		double TA = getPotential(p1);
		double TB = getPotential(p2);

		double u = TB - TA;
		double a = p2.distance(point);
		double b = p1.distance(point);
		double c = p1.distance(p2);

		//double phi = angle(p1, point, p2);
		double cosphi = cosPhi(e1, edge, e2);
		double sinPhi = sinPhi(e1, edge, e2);


		double F = 1.0 / timeCostFunction.costAt(point);

		// solve x2 t^2 + x1 t + x0 == 0
		double x2 = a * a + b * b - 2 * a * b * cosphi;
		double x1 = 2 * b * u * (a * cosphi - b);
		double x0 = b * b * (u * u - F * F * a * a * sinPhi * sinPhi);
		double t = solveQuadratic(x2, x1, x0);

		double inTriangle = (b * (t - u) / t);
		if (u < t && a * cosphi < inTriangle && inTriangle < a / cosphi) {
			return t + TA;
		} else {
			return Math.min(b * F + TA, c * F + TB);
		}
	}

	private double computePotential(final V point, final V point1, final V point2) {

		// see: Sethian, Level Set Methods and Fast Marching Methods, page 124.
		V p1;   // A
		V p2;   // B

		// assuming T(B) > T(A)
		if(getPotential(point1) > getPotential(point2)) {
			p2 = point1;
			p1 = point2;
		}
		else {
			p2 = point2;
			p1 = point1;
		}

		double TA = getPotential(p1);
		double TB = getPotential(p2);

		double u = TB - TA;
		double a = p2.distance(point);
		double b = p1.distance(point);
		double c = p1.distance(p2);

		//double phi = angle(p1, point, p2);
		double cosphi = cosPhi(point1, point, point2);
		double sinPhi = sinPhi(point1, point, point2);


		double F = 1.0 / timeCostFunction.costAt(point);

		// solve x2 t^2 + x1 t + x0 == 0
		double x2 = a * a + b * b - 2 * a * b * cosphi;
		double x1 = 2 * b * u * (a * cosphi - b);
		double x0 = b * b * (u * u - F * F * a * a * sinPhi * sinPhi);
		double t = solveQuadratic(x2, x1, x0);

		double inTriangle = (b * (t - u) / t);
		if (u < t && a * cosphi < inTriangle && inTriangle < a / cosphi) {
			return t + TA;
		} else {
			return Math.min(b * F + TA, c * F + TB);
		}
	}

	private double sinPhi(@NotNull final E p1, @NotNull final E p, @NotNull final E p2) {
		double sinPhi = getMesh().getDoubleData(p, "sinPhis");
		if(sinPhi == 0) {
			sinPhi = Math.sin(angle(p1, p, p2));
			getMesh().setDoubleData(p, "sinPhis", sinPhi);
			return sinPhi;
		}
		else {
			return sinPhi;
		}
	}

	private double cosPhi(@NotNull final V p1, @NotNull final V p, @NotNull final V p2) {
		return Math.cos(GeometryUtils.angle(p1, p, p2));
	}

	private double sinPhi(@NotNull final V p1, @NotNull final V p, @NotNull final V p2) {
		return Math.sin(GeometryUtils.angle(p1, p, p2));
	}

	private double cosPhi(@NotNull final E p1, @NotNull final E p, @NotNull final E p2) {
		double cosPhi = getMesh().getDoubleData(p, "cosPhis");
		if(cosPhi == 0) {
			cosPhi = Math.cos(angle(p1, p, p2));
			getMesh().setDoubleData(p, "cosPhis", cosPhi);
			return cosPhi;
		}
		else {
			return cosPhi;
		}
	}

	private double angle(@NotNull final E p1, @NotNull final E p, @NotNull final E p2) {
		double angle =  getMesh().getDoubleData(p, "angle");
		if(angle == 0) {
			angle = GeometryUtils.angle(getMesh().getVertex(p1), getMesh().getVertex(p), getMesh().getVertex(p2));
			getMesh().setDoubleData(p, "angle", angle);
			return angle;
		}
		else {
			return angle;
		}
	}

	/**
	 * Solves the quadratic equation given by (a*x^2+b*x+c=0).
	 *
	 * @param a a real number in the equation
	 * @param b a real number in the equation
	 * @param c a real number in the equation
	 *
	 * @return the maximum of both solutions, if any.
	 *         Double.MIN_VALUE if there is no real solution i.e. the determinant (det = b^2-4ac is negative)
	 */
	private double solveQuadratic(double a, double b, double c) {
		List<Double> solutions = MathUtil.solveQuadratic(a, b, c);
		double result = Double.MIN_VALUE;
		if (solutions.size() == 2) {
			result =  Math.max(solutions.get(0), solutions.get(1));
		} else if (solutions.size() == 1) {
			result = solutions.get(0);
		}

		return result;

		/*double det = b * b - 4 * a * c;
		if (det < 0) {
			return Double.MIN_VALUE;
		}

		return Math.bound((-b + Math.sqrt(det)) / (2 * a), (-b - Math.sqrt(det))
				/ (2 * a));*/
	}
}
