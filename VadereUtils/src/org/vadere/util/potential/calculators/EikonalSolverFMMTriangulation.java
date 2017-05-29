package org.vadere.util.potential.calculators;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.ConstantLineIterator;
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

import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.math.MathUtil;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.timecost.ITimeCostFunction;
import org.vadere.util.geometry.mesh.iterators.FaceIterator;
import org.vadere.util.triangulation.IPointConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;


public class EikonalSolverFMMTriangulation<P extends PotentialPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements EikonalSolver  {

	private static Logger logger = LogManager.getLogger(EikonalSolverFMMTriangulation.class);

	private ITimeCostFunction timeCostFunction;
	private ITriangulation<P, V, E, F> triangulation;
	private boolean calculationFinished;
	private PriorityQueue<FFMHalfEdge> narrowBand;
	private final Collection<VRectangle> targetAreas;
	private IPointConstructor<P> pointConstructor;
	private IMesh<P, V, E, F> mesh;

	private Comparator<FFMHalfEdge> pointComparator = (he1, he2) -> {
		if (mesh.getPoint(he1.halfEdge).getPotential() < mesh.getPoint(he2.halfEdge).getPotential()) {
			return -1;
		} else if(mesh.getPoint(he1.halfEdge).getPotential() > mesh.getPoint(he2.halfEdge).getPotential()) {
			return 1;
		}
		else {
			return 0;
		}
	};

	public EikonalSolverFMMTriangulation(final Collection<VRectangle> targetAreas,
	                                     final ITimeCostFunction timeCostFunction,
	                                     final ITriangulation<P, V, E, F> triangulation,
	                                     final IPointConstructor<P> pointConstructor
	                                     ) {
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.targetAreas = targetAreas;
		this.pointConstructor = pointConstructor;
		this.narrowBand = new PriorityQueue<>(pointComparator);
		this.mesh = triangulation.getMesh();
	}

	public EikonalSolverFMMTriangulation(final Collection<P> targetPoints,
	                                     final ITimeCostFunction timeCostFunction,
	                                     final ITriangulation<P, V, E, F> triangulation
	) {
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.targetAreas = new ArrayList<>();
		this.narrowBand = new PriorityQueue<>(pointComparator);
		this.mesh = triangulation.getMesh();

		for(P point : targetPoints) {
			F face = triangulation.locateFace(point).get();

			for(E halfEdge : mesh.getEdgeIt(face)) {
				P potentialPoint = mesh.getPoint(halfEdge);
				double distance = point.distance(potentialPoint);

				if(potentialPoint.getPathFindingTag() != PathFindingTag.Undefined) {
					narrowBand.remove(new FFMHalfEdge(halfEdge));
				}

				potentialPoint.setPotential(Math.min(potentialPoint.getPotential(), distance * timeCostFunction.costAt(potentialPoint)));
				potentialPoint.setPathFindingTag(PathFindingTag.Reached);
				narrowBand.add(new FFMHalfEdge(halfEdge));
			}
		}
	}


	private void initializeTargetAreas() {
		for(VRectangle rectangle : targetAreas) {
			VPoint topLeft = new VPoint(rectangle.getX(), rectangle.getY());
			VPoint bottomLeft = new VPoint(rectangle.getX(), rectangle.getMaxY());
			VPoint bottomRight = new VPoint(rectangle.getMaxX(), rectangle.getMaxY());
			VPoint topRight = new VPoint(rectangle.getMaxX(), rectangle.getY());
			ConstantLineIterator lineIterator1 = new ConstantLineIterator(new VLine(topLeft, topRight), 1.0);
			ConstantLineIterator lineIterator2 = new ConstantLineIterator(new VLine(topLeft, bottomLeft), 1.0);
			ConstantLineIterator lineIterator3 = new ConstantLineIterator(new VLine(bottomLeft, bottomRight), 1.0);
			ConstantLineIterator lineIterator4 = new ConstantLineIterator(new VLine(topRight, bottomRight), 1.0);

			List<ConstantLineIterator> lineIterators = Arrays.asList(lineIterator1, lineIterator2, lineIterator3, lineIterator4);

			for(ConstantLineIterator lineIterator : lineIterators) {
				while (lineIterator.hasNext()) {
					IPoint next = lineIterator.next();
					P potentialPoint = pointConstructor.create(next.getX(), next.getY());
					potentialPoint.setPathFindingTag(PathFindingTag.Reached);
					potentialPoint.setPotential(0.0);
					E halfEdge = triangulation.insert(potentialPoint);

					if(halfEdge != null && mesh.getVertex(halfEdge).equals(potentialPoint)) {
						narrowBand.add(new FFMHalfEdge(halfEdge));
					}
					else {
						logger.warn("did not found inserted edge!");
					}
				}
			}

			for(F face : triangulation) {
				for(E potentialPoint : mesh.getEdgeIt(face)) {
					for(VRectangle targetRect : targetAreas) {
						P vertex = mesh.getPoint(potentialPoint);
						if(targetRect.contains(vertex)) {
							vertex.setPotential(0.0);
							vertex.setPathFindingTag(PathFindingTag.Reached);
							narrowBand.add(new FFMHalfEdge(potentialPoint));
						}
					}
				}
			}
		}

		calculate();
	}

	@Override
	public void initialize() {
		initializeTargetAreas();
		/*for(IPoint point : targetPoints) {
			Face<? extends PotentialPoint> face = triangulation.locateFace(point);

			for(HalfEdge<? extends PotentialPoint> halfEdge : face) {
				PotentialPoint potentialPoint = halfEdge.getEnd();
				double distance = point.distance(potentialPoint);

				if(potentialPoint.getPathFindingTag() != PathFindingTag.Undefined) {
					narrowBand.remove(new FFMHalfEdge(halfEdge));
				}

				potentialPoint.setPotential(Math.min(potentialPoint.getPotential(), distance / timeCostFunction.costAt(potentialPoint)));
				potentialPoint.setPathFindingTag(PathFindingTag.Reached);
				narrowBand.add(new FFMHalfEdge(halfEdge));
			}
		}*/

		calculate();
	}

	@Override
	public double getValue(double x, double y) {
		Optional<F> optFace = triangulation.locateFace(x, y);

		double result = Double.MAX_VALUE;
		if(!optFace.isPresent()) {
			logger.warn("no triangle found for coordinates (" + x + "," + y + ")");
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
				//System.out.println(narrowBand.size());
				// poll the point with lowest data value
				FFMHalfEdge ffmHalfEdge = this.narrowBand.poll();
				// add it to the frozen points
				mesh.getPoint(ffmHalfEdge.halfEdge).setPathFindingTag(PathFindingTag.Reached);
				// recalculate the value based on the adjacent triangles
				//double potential = recalculatePoint(ffmHalfEdge.halfEdge);
				//ffmHalfEdge.halfEdge.getEnd().setPotential(Math.min(ffmHalfEdge.halfEdge.getEnd().getPotential(), potential));
				// add narrow points
				setNeighborDistances(ffmHalfEdge.halfEdge);
			}

			this.calculationFinished = true;
		}
	}

	/**
	 * Gets points in the narrow band around p.
	 *
	 * @param halfEdge
	 * @return a set of points in the narrow band that are close to p.
	 */
	private void setNeighborDistances(final E halfEdge) {

		for(E neighbour : mesh.getIncidentEdgesIt(halfEdge)) {
			if(mesh.getPoint(neighbour).getPathFindingTag() == PathFindingTag.Undefined) {
				double potential = recalculatePoint(neighbour);
				// if not, it was not possible to compute a valid potential. TODO?
				if(potential < mesh.getPoint(neighbour).getPotential()) {
					mesh.getPoint(neighbour).setPotential(potential);
					mesh.getPoint(neighbour).setPathFindingTag(PathFindingTag.Reachable);
					narrowBand.add(new FFMHalfEdge(neighbour));
				}
				else {
					//logger.warn("could not set neighbour vertex" + neighbour + "," + neighbour.getFace().isBorder());
				}
			}
			else if(mesh.getPoint(neighbour).getPathFindingTag() == PathFindingTag.Reachable) {
				//double potential = neighbour.getEnd().getPotential();
				double potential = recalculatePoint(neighbour);

				// neighbour might be already in the narrowBand => update it
				if (potential < mesh.getPoint(neighbour).getPotential()) {
					FFMHalfEdge ffmHalfEdge = new FFMHalfEdge(neighbour);
					narrowBand.remove(new FFMHalfEdge(neighbour));
					mesh.getPoint(neighbour).setPotential(potential);
					narrowBand.add(ffmHalfEdge);
				}
			}
		}
	}

	/**
	 * Recalculates the vertex given by the formulas in Sethian-1999.
	 *
	 * @param edge
	 * @return the same point, with a (possibly) changed data value.
	 */
	private double recalculatePoint(final E edge) {
		// loop over all, check whether the point is contained and update its
		// value accordingly
		double potential = Double.MAX_VALUE;

		for(F face : mesh.getAdjacentFacesIt(edge)) {
			if(!mesh.isBoundary(face)) {
				potential = Math.min(computeValue(mesh.getPoint(edge), face), potential);
			}
		}
		return potential;
	}

	/**
	 * Updates a point given a triangle. The point can only be updated if the
	 * triangle triangleContains it and the other two points are in the frozen band.
	 *
	 * @param point
	 * @param face
	 */
	private double computeValue(final P point, final F face) {
		// check whether the triangle does contain useful data
		List<P> points = mesh.getPoints(face);
		E halfEdge = mesh.getEdges(face).stream().filter(p -> mesh.getVertex(p).equals(point)).findAny().get();
		points.removeIf(p -> p.equals(point));

		assert points.size() == 2;
		P p1 = points.get(0);
		P p2 = points.get(1);

		if(feasableForComputation(p1) && feasableForComputation(p2) && !mesh.toTriangle(face).isNonAcute()){
			return computeValue(point, p1, p2);
		}
		return point.getPotential();

		// the general case
		/*double angle = GeometryUtils.angle(p1, point, p2);
		if(((feasableForComputation(p1) && feasableForComputation(p2)) || angle < Math.PI / 2)) {
			return computeValue(point, p1, p2);
		} // no update coming from this face is possible.
		else if(!feasableForComputation(p1) || !feasableForComputation(p2)) {
			PotentialPoint feasablePoint = feasableForComputation(p1) ? p1 : p2;
			HalfEdge<? extends PotentialPoint> he = findPointInCone(halfEdge, p1, p2);
			if(he == null) {
				logger.warn("inconsistent update");
				//return computeValue(point, p1, p2);
				return feasablePoint.getPotential();
			}
			else {
				logger.info("consistent update " + point.distance(he.getEnd()));
				assert GeometryUtils.angle(feasablePoint, point, he.getEnd()) < Math.PI / 2;
				return computeValue(point, feasablePoint, he.getEnd());
			}
		}
		else { // both neighbours are computed but the angle is to large.
			return point.getPotential();
		}*/
	}

	private boolean feasableForComputation(final P p){
		//return p.getPathFindingTag().frozen;
		return p.getPathFindingTag() == PathFindingTag.Reachable || p.getPathFindingTag() == PathFindingTag.Reached;
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
					.filter(he -> feasableForComputation(mesh.getPoint(he)))
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
				if (feasableForComputation(vertex) && cone.contains(new VPoint(vertex))) {
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

	private double computeValue(final P point, final P point1, final P point2) {

		/*if ((Double.isInfinite(p1.getPotential()) && Double.isInfinite((p2.getPotential())))
				|| (Double.isInfinite(p1.getPotential()) && Double.isInfinite(point.getPotential()))
				|| (Double.isInfinite(p2.getPotential()) && Double.isInfinite(point.getPotential()))) {
			return point.getPotential();
		}*/

		// check whether they are in the frozen set. only if they are, we can
		// continue.
		// if(this.frozenPoints.triangleContains(points.first()) &&
		// this.frozenPoints.triangleContains(points.last()))

		/*if(
				(p1.getPathFindingTag() == PathFindingTag.Reached || p1.getPathFindingTag() == PathFindingTag.Reachable)
						&& (p2.getPathFindingTag() == PathFindingTag.Reached || p2.getPathFindingTag() == PathFindingTag.Reachable))
		{*/
		//if(p1.getPathFindingTag().frozen && p2.getPathFindingTag().frozen) {
			// see: Sethian, Level Set Methods and Fast Marching Methods, page
			// 124.
		P p1;
		P p2;

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
		double a = p1.distance(point);
		double b = p2.distance(point);
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

	/**
	 * We require a half-edge that has an equals which only depends on the end-vertex.
	 */
	private class FFMHalfEdge {
		private E halfEdge;

		public FFMHalfEdge(final E halfEdge){
			this.halfEdge = halfEdge;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			FFMHalfEdge that = (FFMHalfEdge) o;

			return mesh.getVertex(halfEdge).equals(mesh.getVertex(that.halfEdge));
		}

		@Override
		public int hashCode() {
			return mesh.getVertex(halfEdge).hashCode();
		}

		@Override
		public String toString() {
			return halfEdge.toString();
		}
	}
}
