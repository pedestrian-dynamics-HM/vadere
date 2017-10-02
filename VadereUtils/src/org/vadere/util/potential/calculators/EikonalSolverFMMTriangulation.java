package org.vadere.util.potential.calculators;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCone;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.math.MathUtil;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.timecost.ITimeCostFunction;
import org.vadere.util.geometry.mesh.iterators.FaceIterator;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * This class computes the travelling time T(x) using the Fast Marching Method for arbitrary triangulated meshes.
 * The quality of the result depends on the quality of the triangulation. Therefore, the triangualtion shouldn't contain
 * too many non-acute triangles.
 *
 * @param <P>   the type of the points of the triangulation (they have to be an extension of potential points)
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 */
public class EikonalSolverFMMTriangulation<P extends PotentialPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements EikonalSolver  {

	private static Logger logger = LogManager.getLogger(EikonalSolverFMMTriangulation.class);

	private ITimeCostFunction timeCostFunction;
	private ITriangulation<P, V, E, F> triangulation;
	private boolean calculationFinished;
	private PriorityQueue<V> narrowBand;
	private Collection<VRectangle> targetAreas;
	private IMesh<P, V, E, F> mesh;

	/**
	 * Comparator for the heap. Vertices of points with small potentials are at the top of the heap.
	 */
	private Comparator<V> pointComparator = (v1, v2) -> {
		P p1 = mesh.getPoint(v1);
		P p2 = mesh.getPoint(v2);
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
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param targetPoints      Points where the propagating wave starts i.e. points that are part of the target area.
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public EikonalSolverFMMTriangulation(@NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final Collection<IPoint> targetPoints,
	                                     @NotNull final ITriangulation<P, V, E, F> triangulation
	) {
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.narrowBand = new PriorityQueue<>(pointComparator);
		this.mesh = triangulation.getMesh();

		for(IPoint point : targetPoints) {
			F face = triangulation.locateFace(point.getX(), point.getY()).get();
			initialFace(face, p -> point.distance(p));
		}
	}

	/**
	 * Constructor for certain target shapes.
	 * @param targetShapes      shapes that define the whole target area.
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public EikonalSolverFMMTriangulation(@NotNull final Collection<VShape> targetShapes,
	                                     @NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final ITriangulation<P, V, E, F> triangulation
	) {
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.narrowBand = new PriorityQueue<>(pointComparator);
		this.mesh = triangulation.getMesh();

		for(VShape shape : targetShapes) {
			mesh.streamFaces()
					.filter(f -> !mesh.isBoundary(f))
					.filter(f -> shape.intersect(mesh.toTriangle(f)))
					.forEach(f -> initialFace(f, p -> Math.max(shape.distance(p), 0)));
		}
	}

	/**
	 * Constructor for certain vertices of the triangulation.
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 * @param targetVertices    vertices which are part of the triangulation where the propagating wave starts i.e. points that are part of the target area.
	 */
	public EikonalSolverFMMTriangulation(@NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final ITriangulation<P, V, E, F> triangulation,
	                                     @NotNull final Collection<V> targetVertices,
                                         @NotNull final IDistanceFunction distFunc
	) {
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.targetAreas = new ArrayList<>();
		this.narrowBand = new PriorityQueue<>(pointComparator);
		this.mesh = triangulation.getMesh();

		for(V vertex : targetVertices) {
			P potentialPoint = mesh.getPoint(vertex);
			double distance = -distFunc.apply(potentialPoint);

			if(potentialPoint.getPathFindingTag() != PathFindingTag.Undefined) {
				narrowBand.remove(vertex);
			}

			potentialPoint.setPotential(Math.min(potentialPoint.getPotential(), distance / timeCostFunction.costAt(potentialPoint)));
			potentialPoint.setPathFindingTag(PathFindingTag.Reached);
			narrowBand.add(vertex);

			for(V v : triangulation.getMesh().getAdjacentVertexIt(vertex)) {
			    if(!narrowBand.contains(v)) {
                    P potentialP = mesh.getPoint(v);

                    double dist = Math.max(-distFunc.apply(potentialP), 0);
                    logger.info(dist);
                    potentialP.setPotential(Math.min(potentialP.getPotential(), dist / timeCostFunction.costAt(potentialP)));
                    potentialP.setPathFindingTag(PathFindingTag.Reachable);
                    narrowBand.add(v);
                }
            }
		}
	}

	/**
	 * Computes and sets the potential of all points of a face based on the distance function.
	 * @param face              the face
	 * @param distanceFunction  the distance function
	 */
	private void initialFace(@NotNull final F face, @NotNull final IDistanceFunction distanceFunction) {
		for(V vertex : mesh.getVertexIt(face)) {
			P potentialPoint = mesh.getPoint(vertex);
			double distance = distanceFunction.apply(potentialPoint);

			if(potentialPoint.getPathFindingTag() != PathFindingTag.Undefined) {
				narrowBand.remove(vertex);
			}

			potentialPoint.setPotential(Math.min(potentialPoint.getPotential(), distance * timeCostFunction.costAt(potentialPoint)));
			potentialPoint.setPathFindingTag(PathFindingTag.Reached);
			narrowBand.add(vertex);
		}
	}

	@Override
	public void initialize() {
		calculate();
	}

	@Override
	public double getValue(final double x, final double y) {
		Optional<F> optFace = triangulation.locateFace(x, y);

		double result = -1.0;
		if(!optFace.isPresent()) {
			//logger.warn("no face found for coordinates (" + x + "," + y + ")");
		}
		else if(optFace.isPresent() && triangulation.getMesh().isBoundary(optFace.get())) {
		//	logger.warn("no triangle found for coordinates (" + x + "," + y + ")");
		}
		else {
			result = InterpolationUtil.barycentricInterpolation(mesh.getPoints(optFace.get()), x, y);
		}
		return result;
	}


	/**
	 * Calculate the fast marching solution. This is called only once,
	 * subsequent calls only return the result of the first.
	 */
	private void calculate() {
		if (!calculationFinished) {
			while (this.narrowBand.size() > 0) {
				V vertex = this.narrowBand.poll();
				mesh.getPoint(vertex).setPathFindingTag(PathFindingTag.Reached);
				updatePotentialOfNeighbours(vertex);
			}
			this.calculationFinished = true;
		}
	}

	/**
	 * Updates the potential values of all neighbours of a certain vertex inside the triangulation.
	 *
	 * @param vertex    the vertex
	 */
	private void updatePotentialOfNeighbours(@NotNull final V vertex) {
		for(V neighbour : mesh.getAdjacentVertexIt(vertex)) {
			updatePotential(neighbour);
		}
	}

	/**
	 * Updates the potential values a certain vertex by recomputing its potential.
	 * @param vertex
	 */
	private void updatePotential(@NotNull final V vertex) {
		double potential = recomputePotential(vertex);
		P potentialPoint = mesh.getPoint(vertex);
		if(potential < potentialPoint.getPotential()) {
			if(potentialPoint.getPathFindingTag() == PathFindingTag.Reachable) {
				narrowBand.remove(vertex);
			}

			potentialPoint.setPotential(potential);
			potentialPoint.setPathFindingTag(PathFindingTag.Reachable);
			narrowBand.add(vertex);
		}

		if(potentialPoint.getPathFindingTag() == PathFindingTag.Undefined) {
			logger.warn("could not set neighbour vertex" + vertex);
		}
	}

	/**
	 * Recomputes the potential of a potential point of a certain vertex by
	 * computing all possible potentials for each neighbouring points and returns it.
	 * If the new potential is not smaller than the old potential of the point, the old
	 * value will be returned.
	 *
	 * @param vertex    the vertex which represents the potential point.
	 * @return the recomputed potential of a potential point
	 */
	private double recomputePotential(@NotNull final V vertex) {
		// loop over all, check whether the point is contained and update its
		// value accordingly
		double potential = Double.MAX_VALUE;

		for(F face : mesh.getAdjacentFacesIt(vertex)) {
			if(!mesh.isBoundary(face)) {
				potential = Math.min(computePotential(mesh.getPoint(vertex), face), potential);
			}
		}

		return potential;
	}


    public boolean isNonAcute(@NotNull final E edge) {
	    VPoint p1 = mesh.toPoint(mesh.getPrev(edge));
	    VPoint p2 = mesh.toPoint(edge);
	    VPoint p3 = mesh.toPoint(mesh.getNext(edge));

        double angle1 = GeometryUtils.angle(p1, p2, p3);

        // non-acute triangle
        double rightAngle = Math.PI/2;
        return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
    }

	/**
	 * Updates a point given a triangle. The point can only be updated if the
	 * triangle triangleContains it and the other two points are in the frozen band.
	 *
	 * @param point
	 * @param face
	 */
	private double computePotential(@NotNull final P point, @NotNull final F face) {
		// check whether the triangle does contain useful data
		List<E> edges = mesh.getEdges(face);
        E halfEdge = edges.stream().filter(e -> mesh.getPoint(e).equals(point)).findAny().get();
        edges.removeIf(edge -> mesh.getPoint(edge).equals(point));

		assert edges.size() == 2;

		P p1 = mesh.getPoint(edges.get(0));
		P p2 = mesh.getPoint(edges.get(1));

		if(isFeasibleForComputation(p1) && isFeasibleForComputation(p2)) {
			if(!isNonAcute(halfEdge)) {
				return computePotential(point, p1, p2);
			} // we only try to find a virtual vertex if both points are already frozen
			else {
				//logger.info("special case for non-acute triangle");
				Optional<P> optPoint = walkToFeasiblePoint(halfEdge, face);

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

	private boolean isFeasibleForComputation(final P p){
		//return p.getPathFindingTag().frozen;
		return p.getPathFindingTag() == PathFindingTag.Reachable || p.getPathFindingTag() == PathFindingTag.Reached;
	}

	private Optional<P> walkToFeasiblePoint(@NotNull final E halfEdge, @NotNull final F face) {
		assert mesh.toTriangle(face).isNonAcute();

		E next = mesh.getNext(halfEdge);
		E prev = mesh.getPrev(halfEdge);

		VPoint p = new VPoint(mesh.getVertex(halfEdge));
		VPoint pNext = new VPoint(mesh.getVertex(next));
		VPoint pPrev = new VPoint(mesh.getVertex(prev));

		VPoint direction1 = pNext.subtract(p).rotate(+Math.PI/2);
		VPoint direction2 = pPrev.subtract(p).rotate(-Math.PI/2);

        //logger.info(p + ", " + pNext + ", " + pPrev);
        //logger.info(direction1 + ", " + direction2);

        Predicate<E> isPointInCone = e ->
        {
            VPoint point = mesh.toPoint(e);
            return  GeometryUtils.isLeftOf(p, p.add(direction2), point) &&
                    GeometryUtils.isRightOf(p, p.add(direction1), point);
        };

        Predicate<E> isEdgeInCone = e -> isPointInCone.test(e) || isPointInCone.test(mesh.getPrev(e));

        F destination = triangulation.straightWalk2D(halfEdge, face, direction1, isEdgeInCone);

        assert !destination.equals(face);

        if(!mesh.isBoundary(destination)) {
            return mesh.streamEdges(destination).filter(e -> isPointInCone.test(e)).map(v -> mesh.getPoint(v)).findAny();
        }
        else {
            logger.warn("walked to boundary");
            return Optional.empty();
        }

		/*Predicate<V> feasableVertexPred = v -> GeometryUtils.isLeftOf(p, p.add(direction2), mesh.toPoint(v)) && GeometryUtils.isRightOf(p, p.add(direction1), mesh.toPoint(v));
		Predicate<E> feasableEdgePred = e -> feasableVertexPred.test(mesh.getVertex(halfEdge));
		Predicate<E> stopCondition = e -> {
			VPoint midPoint = mesh.toLine(e).midPoint();
			return mesh.isAtBoundary(e) || feasableEdgePred.test(e) || GeometryUtils.isRightOf(midPoint, midPoint.add(direction1), mesh.toPoint(e));
		};

		F newFace = triangulation.straightWalk2D(halfEdge, direction1, stopCondition);

		if(!mesh.isBoundary(newFace)) {
			return mesh.streamVertices(newFace).filter(v -> feasableVertexPred.test(v)).map(v -> mesh.getPoint(v)).findAny();
		}
		else {
			return Optional.empty();
		}*/
	}

	private E findIntersectionPoint(final E halfEdge, final P acceptedPoint, final P unacceptedPoint) {
		P point = mesh.getPoint(halfEdge);
		VTriangle triangle = new VTriangle(new VPoint(point), new VPoint(acceptedPoint), new VPoint(unacceptedPoint));

		// 1. construct the acute cone
		VPoint direction = triangle.getIncenter().subtract(point);
		double angle = Math.PI - GeometryUtils.angle(acceptedPoint, point, unacceptedPoint);
		VPoint origin = new VPoint(point);
		VCone cone = new VCone(origin, direction, angle);
		E minHe = null;

		//
		/*Predicate<Face<? extends PotentialPoint>> pred = f -> {
			List<HalfEdge<? extends PotentialPoint>> pointList = f.stream()
				//.filter(he -> he.getEnd().getPathFindingTag().frozen)
				.filter(he -> !he.getEnd().equals(acceptedPoint))
				.filter(he -> !he.getEnd().equals(point))
				.collect(Collectors.toList());

			for(HalfEdge<? extends PotentialPoint> he : pointList) {
				VTriangle vTriangle = new VTriangle(new VPoint(he.getEnd()), new VPoint(point), new VPoint(acceptedPoint));
				if(cone.intersect(vTriangle)) {
					return true;
				}
			}
			return false;
		};*/
		//



		// 2. search for the nearest point inside the cone
		FaceIterator<P, V, E, F> faceIterator = new FaceIterator<>(mesh);
		while (faceIterator.hasNext()) {
			F face = faceIterator.next();
			List<E> pointList = mesh.getEdges(face).stream()
					.filter(he -> isFeasibleForComputation(mesh.getPoint(he)))
					.filter(he -> !mesh.getVertex(he).equals(acceptedPoint))
					.filter(he -> !mesh.getVertex(he).equals(point))
					.collect(Collectors.toList());

			for(E he : pointList) {
				if(cone.contains(new VPoint(mesh.getVertex(he)))) {
					if(minHe == null || mesh.getPoint(minHe).getPotential() > mesh.getPoint(he).getPotential()) {
						minHe = he;
						return minHe;
					}
				}
			}
		}

		return minHe;
	}


	//TODO: refactoring!
	private E findPointInCone(final E halfEdge, final P p1, final P p2) {
		P point = mesh.getPoint(halfEdge);
		VTriangle triangle = new VTriangle(new VPoint(point), new VPoint(p1), new VPoint(p2));

		// 1. construct the acute cone
		VPoint direction = triangle.getIncenter().subtract(point);
		double angle = Math.PI - GeometryUtils.angle(p1, point, p2);
		VPoint origin = new VPoint(point);
		VCone cone = new VCone(origin, direction, angle);

		// 2. search for the nearest point inside the cone
		Set<F> visitedFaces = new HashSet<>();
		LinkedList<E> pointList = new LinkedList<>();

		E edge = mesh.getNext(mesh.getTwin(mesh.getPrev(halfEdge)));
		pointList.add(edge);
		visitedFaces.add(mesh.getFace(edge));

		while (!pointList.isEmpty()) {
			E candidate = pointList.removeFirst();

			// we can not search further since we reach the boundary.
			if (!mesh.isBoundary(candidate)) {
				P vertex = mesh.getPoint(candidate);
				if (isFeasibleForComputation(vertex) && cone.contains(new VPoint(vertex))) {
					return candidate;
				} else if(cone.contains(new VPoint(vertex))) {
					E newCandidate = mesh.getNext(mesh.getTwin(candidate));
					if (!visitedFaces.contains(mesh.getFace(newCandidate))) {
						visitedFaces.add(mesh.getFace(newCandidate));
						pointList.add(newCandidate);
					}

					newCandidate = mesh.getNext(mesh.getTwin(mesh.getNext(candidate)));
					if (!visitedFaces.contains(mesh.getFace(newCandidate))) {
						visitedFaces.add(mesh.getFace(newCandidate));
						pointList.add(newCandidate);
					}
				}
				else {
					P v1 = mesh.getPoint(candidate);
					P v2 = mesh.getPoint(mesh.getPrev(candidate));
					P v3 = mesh.getPoint(mesh.getNext(candidate));

					VLine line1 = new VLine(new VPoint(v1), new VPoint(v2));
					VLine line2 = new VLine(new VPoint(v1), new VPoint(v3));

					if (cone.overlapLineSegment(line1)) {
						E newCandidate = mesh.getNext(mesh.getTwin(candidate));

						if (!visitedFaces.contains(mesh.getFace(newCandidate))) {
							visitedFaces.add(mesh.getFace(newCandidate));
							pointList.add(newCandidate);
						}
					}

					if (cone.overlapLineSegment(line2)) {
						E newCandidate = mesh.getNext(mesh.getTwin(mesh.getNext(candidate)));

						if (!visitedFaces.contains(mesh.getFace(newCandidate))) {
							visitedFaces.add(mesh.getFace(newCandidate));
							pointList.add(newCandidate);
						}
					}
				}
			}
			else {
				logger.warn("boundary reached!");
			}
		}

		logger.warn("no virtual vertex was found");
		return null;
	}

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

		double F = 1.0 / this.timeCostFunction.costAt(point);

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
	 * Solves the quadratic equation given by a x^2+bx+c=0.
	 *
	 * @param a
	 * @param b
	 * @param c
	 * @return the maximum of both solutions, if any. If det=b^2-4ac < 0, it
	 *         returns Double.MIN_VALUE
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
