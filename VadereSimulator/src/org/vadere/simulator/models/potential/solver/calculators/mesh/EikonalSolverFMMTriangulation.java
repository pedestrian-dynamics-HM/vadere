package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.utils.debug.DebugGui;
import org.vadere.meshing.utils.debug.SimpleTriCanvas;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.math.MathUtil;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.util.data.cellgrid.IPotentialPoint;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * This class computes the traveling time T using the fast marching method for arbitrary triangulated meshes.
 * The quality of the result depends on the quality of the triangulation. For a high accuracy the triangulation
 * should not contain too many non-acute triangles.
 *
 * @param <P>   the type of the points of the triangulation extending {@link IPotentialPoint}
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 */
public class EikonalSolverFMMTriangulation<P extends IPotentialPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements EikonalSolver {

    private static Logger logger = LogManager.getLogger(EikonalSolverFMMTriangulation.class);

    static {
    	logger.setLevel(Level.INFO);
    }

	/**
	 * The time cost function defined on the geometry.
	 */
	private ITimeCostFunction timeCostFunction;

	/**
	 * The triangulation the solver uses.
	 */
    private IIncrementalTriangulation<P, V, E, F> triangulation;

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
        P p1 = getMesh().getPoint(v1);
        P p2 = getMesh().getPoint(v2);
        if (p1.getPotential() < p2.getPotential()) {
            return -1;
        } else if(p1.getPotential() > p2.getPotential()) {
            return 1;
        }
        else {
            return 0;
        }
    };

    // Note: The order of arguments in the constructors are exactly as they are since the generic type of a collection is only known at run-time!

    /**
     * Constructor for certain target points.
     *
     * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
     * @param targetPoints      Points where the propagating wave starts i.e. points that are part of the target area.
     * @param triangulation     the triangulation the propagating wave moves on.
     */
    public EikonalSolverFMMTriangulation(@NotNull final ITimeCostFunction timeCostFunction,
                                         @NotNull final Collection<IPoint> targetPoints,
                                         @NotNull final IIncrementalTriangulation<P, V, E, F> triangulation
    ) {
        this.triangulation = triangulation;
        this.calculationFinished = false;
        this.timeCostFunction = timeCostFunction;
        this.narrowBand = new PriorityQueue<>(pointComparator);

        for(IPoint point : targetPoints) {
            F face = triangulation.locateFace(point.getX(), point.getY()).get();
            initialFace(face, p -> point.distance(p));
        }
    }

    /**
     * Constructor for certain target shapes.
     *
     * @param targetShapes      shapes that define the whole target area.
     * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
     * @param triangulation     the triangulation the propagating wave moves on.
     */
    public EikonalSolverFMMTriangulation(@NotNull final Collection<VShape> targetShapes,
                                         @NotNull final ITimeCostFunction timeCostFunction,
                                         @NotNull final IIncrementalTriangulation<P, V, E, F> triangulation
    ) {
        this.triangulation = triangulation;
        this.calculationFinished = false;
        this.timeCostFunction = timeCostFunction;
        this.narrowBand = new PriorityQueue<>(pointComparator);

        for(VShape shape : targetShapes) {
            getMesh().streamFaces()
                    .filter(f -> !getMesh().isBoundary(f))
                    .filter(f -> shape.intersects(getMesh().toTriangle(f)))
                    .forEach(f -> initialFace(f, p -> Math.max(shape.distance(p), 0)));
        }
    }

    /**
     * Constructor for certain vertices of the triangulation.
     *
     * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
     * @param triangulation     the triangulation the propagating wave moves on.
     * @param targetVertices    vertices which are part of the triangulation where the propagating wave starts i.e. points that are part of the target area.
     * @param distFunc          the distance function (distance to the target) which is negative inside and positive outside the area of interest
     */
    public EikonalSolverFMMTriangulation(@NotNull final ITimeCostFunction timeCostFunction,
                                         @NotNull final IIncrementalTriangulation<P, V, E, F> triangulation,
                                         @NotNull final Collection<V> targetVertices,
                                         @NotNull final IDistanceFunction distFunc
    ) {
        this.triangulation = triangulation;
        this.calculationFinished = false;
        this.timeCostFunction = timeCostFunction;
        this.narrowBand = new PriorityQueue<>(pointComparator);

        for(V vertex : targetVertices) {
            P potentialPoint = getMesh().getPoint(vertex);
            double distance = -distFunc.apply(potentialPoint);

            if(potentialPoint.getPathFindingTag() != PathFindingTag.Undefined) {
                narrowBand.remove(vertex);
            }

            potentialPoint.setPotential(Math.min(potentialPoint.getPotential(), distance / timeCostFunction.costAt(potentialPoint)));
            potentialPoint.setPathFindingTag(PathFindingTag.Reached);
            narrowBand.add(vertex);

            for(V v : triangulation.getMesh().getAdjacentVertexIt(vertex)) {
	            P potentialP = getMesh().getPoint(v);

	            if(potentialP.getPathFindingTag() == PathFindingTag.Undefined) {
                    double dist = Math.max(-distFunc.apply(potentialP), 0);
                    logger.debug("T at " + potentialP + " = " + dist);
                    potentialP.setPotential(Math.min(potentialP.getPotential(), dist / timeCostFunction.costAt(potentialP)));
                    potentialP.setPathFindingTag(PathFindingTag.Reachable);
                    narrowBand.add(v);
                }
            }
        }
    }

    /**
     * Computes and sets the potential of all points of a face based on the distance function.
     *
     * @param face              the face
     * @param distanceFunction  the distance function
     */
    private void initialFace(@NotNull final F face, @NotNull final IDistanceFunction distanceFunction) {
        for(V vertex : getMesh().getVertexIt(face)) {
            P potentialPoint = getMesh().getPoint(vertex);
            double distance = distanceFunction.apply(potentialPoint);

            if(potentialPoint.getPathFindingTag() != PathFindingTag.Undefined) {
                narrowBand.remove(vertex);
            }

            potentialPoint.setPotential(Math.min(potentialPoint.getPotential(), distance * timeCostFunction.costAt(potentialPoint)));
            potentialPoint.setPathFindingTag(PathFindingTag.Reached);
            narrowBand.add(vertex);
        }
    }

	/**
	 * Calculate the fast marching solution. This is called only once,
	 * subsequent calls only return the result of the first.
	 */
    @Override
    public void initialize() {
	    if (!calculationFinished) {
		    while (narrowBand.size() > 0) {
			    V vertex = narrowBand.poll();
			    getMesh().getPoint(vertex).setPathFindingTag(PathFindingTag.Reached);
			    updatePotentialOfNeighbours(vertex);
		    }
		    calculationFinished = true;
	    }
    }

    // unknownPenalty is ignored.
	@Override
	public double getPotential(@NotNull final IPoint pos, final double unknownPenalty, final double weight) {
		return weight * getPotential(pos.getX(), pos.getY());
	}

	@Override
    public Function<IPoint, Double> getPotentialField() {
	    IIncrementalTriangulation<P, V, E, F> clone = triangulation.clone();
	    return p -> getPotential(clone, p.getX(), p.getY());
    }

    @Override
    public double getPotential(final double x, final double y) {
        return getPotential(triangulation, x, y);
    }

	@Override
	public IMesh<? extends IPotentialPoint, ?, ?, ?> getDiscretization() {
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
	 * @param <P>   the type of the points of the triangulation extending {@link IPotentialPoint}
	 * @param <V>   the type of the vertices of the triangulation
	 * @param <E>   the type of the half-edges of the triangulation
	 * @param <F>   the type of the faces of the triangulation
	 *
	 * @return the interpolated value of the traveling time T at (x, y)
	 */
    private static <P extends IPotentialPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> double getPotential(
    		@NotNull final IIncrementalTriangulation<P, V, E, F> triangulation,
		    final double x,
		    final double y) {

	    Optional<F> optFace = triangulation.locateFace(x, y);

	    double result = Double.MAX_VALUE;
	    if(!optFace.isPresent()) {
		    //logger.warn("no face found for coordinates (" + x + "," + y + ")");
	    }
	    else if(optFace.isPresent() && triangulation.getMesh().isBoundary(optFace.get())) {
		    //	logger.warn("no triangle found for coordinates (" + x + "," + y + ")");
	    }
	    else {
		    result = InterpolationUtil.barycentricInterpolation(triangulation.getMesh().getPoints(optFace.get()), x, y);
	    }
	    return result;
    }

    private IMesh<P, V, E, F> getMesh() {
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
        P potentialPoint = getMesh().getPoint(vertex);
        if(potential < potentialPoint.getPotential()) {
            if(potentialPoint.getPathFindingTag() == PathFindingTag.Reachable) {
                narrowBand.remove(vertex);
            }

            potentialPoint.setPotential(potential);
            potentialPoint.setPathFindingTag(PathFindingTag.Reachable);
            narrowBand.add(vertex);
        }

        if(potentialPoint.getPathFindingTag() == PathFindingTag.Undefined) {
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

        for(F face : getMesh().getAdjacentFacesIt(vertex)) {
            if(!getMesh().isBoundary(face)) {
                potential = Math.min(computePotential(getMesh().getPoint(vertex), face), potential);
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

    /**
     * Updates a point given a triangle. The point can only be updated if the
     * triangle triangleContains it and the other two points are in the frozen band.
     *
     * @param point a point for which the potential should be re-computed
     * @param face  a face neighbouring the point
     */
    private double computePotential(@NotNull final P point, @NotNull final F face) {
        // check whether the triangle does contain useful data
        List<E> edges = getMesh().getEdges(face);
        E halfEdge = edges.stream().filter(e -> getMesh().getPoint(e).equals(point)).findAny().get();
        edges.removeIf(edge -> getMesh().getPoint(edge).equals(point));

        assert edges.size() == 2;

        P p1 = getMesh().getPoint(edges.get(0));
        P p2 = getMesh().getPoint(edges.get(1));

        if(isFeasibleForComputation(p1) && isFeasibleForComputation(p2)) {

        	if(!isNonAcute(halfEdge)) {
        		double potential = computePotential(point, p1, p2);
        		//logger.info("compute potential " + potential);
                return computePotential(point, p1, p2);
            } // we only try to find a virtual vertex if both points are already frozen
            else {
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
            }
        }

        return Double.MAX_VALUE;
    }

	/**
	 * Defines the porperties for which a point is used to compute the traveling time of neighbouring points.
	 *
	 * @param p the point which is tested
	 *
	 * @return true if the point can be used for computation, false otherwise
	 */
	private boolean isFeasibleForComputation(final P p){
        //return p.getPathFindingTag().frozen;
        return p.getPathFindingTag() == PathFindingTag.Reachable || p.getPathFindingTag() == PathFindingTag.Reached;
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
	private Optional<P> walkToNumericalSupport(@NotNull final E halfEdge, @NotNull final F face) {
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
            return getMesh().streamEdges(destination).filter(e -> isPointInCone.test(e)).map(v -> getMesh().getPoint(v)).findAny();
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


	/**
	 * Computes the traveling time T at <tt>point</tt> by using the neighbouring points <tt>point1</tt> and <tt>point2</tt>.
	 *
	 * @param point     the point for which the traveling time is computed
	 * @param point1    one neighbouring point
	 * @param point2    another neighbouring point
	 *
	 * @return the traveling time T at <tt>point</tt> by using the triangle (point, point1, point2) for the computation
	 */
	private double computePotential(final P point, final P point1, final P point2) {

        // see: Sethian, Level Set Methods and Fast Marching Methods, page 124.
        P p1;   // A
        P p2;   // B

        // assuming T(B) > T(A)
        if(point1.getPotential() > point2.getPotential()) {
            p2 = point1;
            p1 = point2;
        }
        else {
            p2 = point2;
            p1 = point1;
        }

        double TA = p1.getPotential();
        double TB = p2.getPotential();

        double u = TB - TA;
        double a = p2.distance(point);
        double b = p1.distance(point);
        double c = p1.distance(p2);

        double phi = GeometryUtils.angle(p1, point, p2);
        double cosphi = Math.cos(phi);

        double F = 1.0 / timeCostFunction.costAt(point);

        // solve x2 t^2 + x1 t + x0 == 0
        double x2 = a * a + b * b - 2 * a * b * cosphi;
        double x1 = 2 * b * u * (a * cosphi - b);
        double x0 = b * b * (u * u - F * F * a * a * Math.sin(phi) * Math.sin(phi));
        double t = solveQuadratic(x2, x1, x0);

        double inTriangle = (b * (t - u) / t);
        if (u < t && a * cosphi < inTriangle && inTriangle < a / cosphi) {
            return t + TA;
        } else {
            return Math.min(b * F + TA, c * F + TB);
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
