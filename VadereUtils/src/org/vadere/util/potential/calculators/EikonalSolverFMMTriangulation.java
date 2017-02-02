package org.vadere.util.potential.calculators;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.data.HalfEdge;
import org.vadere.util.geometry.data.Triangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.potential.timecost.ITimeCostFunction;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;


public class EikonalSolverFMMTriangulation implements EikonalSolver  {

	private ITimeCostFunction timeCostFunction;
	private Triangulation<PotentialPoint> triangulation;
	private boolean calculationFinished;
	private PriorityQueue<HalfEdge<PotentialPoint>> narrowBand;
	private final Collection<HalfEdge<PotentialPoint>> targetPoints;

	private Comparator<HalfEdge<PotentialPoint>> pointComparator = (he1, he2) -> {
		if (he1.getEnd().getPotential() < he2.getEnd().getPotential()) {
			return -1;
		} else if(he1.getEnd().getPotential() > he2.getEnd().getPotential()) {
			return 1;
		}
		else {
			return 0;
		}
	};

	public EikonalSolverFMMTriangulation(final Collection<HalfEdge<PotentialPoint>> targetPoints,
	                                     final ITimeCostFunction timeCostFunction,
	                                     final Triangulation<PotentialPoint> triangulation) {
		this.triangulation = triangulation;
		this.calculationFinished = false;
		this.timeCostFunction = timeCostFunction;
		this.targetPoints = targetPoints;
	}

	@Override
	public void initialize() {
		this.narrowBand = new PriorityQueue<>(pointComparator);

		for(HalfEdge<PotentialPoint> halfEdge : targetPoints) {
			halfEdge.getEnd().freeze();
			halfEdge.getEnd().setPotential(0.0);
			narrowBand.add(halfEdge);
		}
	}

	@Override
	public double getValue(double x, double y) {
		Face<PotentialPoint> triangle = triangulation.locate(new VPoint(x, y));
		if(triangle != null) {
			return InterpolationUtil.barycentricInterpolation(triangle, x, y);
		}
		else {
			return Double.MAX_VALUE;
		}
	}


	/**
	 * Calculate the fast marching solution. This is called only once,
	 * subsequent calls only return the result of the first.
	 */
	public void calculate() {
		if (!calculationFinished) {
			while (this.narrowBand.size() > 0) {
				// poll the point with lowest data value
				HalfEdge<PotentialPoint> nextPoint = this.narrowBand.poll();
				// add it to the frozen points
				nextPoint.getEnd().freeze();
				// recalculate the value based on the adjacent triangles
				recalculatePoint(nextPoint);
				// add narrow points
				getNarrowPoints(nextPoint);
			}

			this.calculationFinished = true;
		}
	}

	/**
	 * Gets points in the narrow band around p.
	 *
	 * @param p
	 * @return a set of points in the narrow band that are close to p.
	 */
	private void getNarrowPoints(final HalfEdge<PotentialPoint> p) {
		// remove frozen points
		Iterator<HalfEdge<PotentialPoint>> it = p.incidentPointIterator();

		while (it.hasNext()) {
			HalfEdge<PotentialPoint> neighbour = it.next();
			if(!neighbour.getEnd().isFrozen()) {
				double potential = neighbour.getEnd().getPotential();
				recalculatePoint(neighbour);
				if (potential > neighbour.getEnd().getPotential()) {
					this.narrowBand.add(neighbour);
				}
			}
		}
	}

	/**
	 * Recalculates the vertex given by the formulas in Sethian-1999.
	 *
	 * @param point
	 * @return the same point, with a (possibly) changed data value.
	 */
	private void recalculatePoint(final HalfEdge<PotentialPoint> point) {
		// loop over all, check whether the point is contained and update its
		// value accordingly
		Iterator<Face<PotentialPoint>> it = point.inciedentFaceIterator();
		while (it.hasNext()) {
			updatePoint(point, it.next());
		}
	}

	/**
	 * Updates a point given a triangle. The point can only be updated if the
	 * triangle contains it and the other two points are in the frozen band.
	 *
	 * @param halfEdge
	 * @param face
	 */
	private void updatePoint(final HalfEdge<PotentialPoint> halfEdge, final Face<PotentialPoint> face) {
		// check whether the triangle does contain useful data
		VTriangle triangle = halfEdge.getFace().toTriangle();
		List<PotentialPoint> points = face.getPoints();
		points.removeIf(p -> p.equals(halfEdge.getEnd()));

		assert points.size() == 2;

		PotentialPoint p1 = points.get(0);
		PotentialPoint p2 = points.get(1);
		PotentialPoint point = halfEdge.getEnd();

		if ((Double.isInfinite(p1.getPotential()) && Double.isInfinite((p2.getPotential())))
				|| (Double.isInfinite(p1.getPotential()) && Double.isInfinite(point.getPotential()))
				|| (Double.isInfinite(p2.getPotential()) && Double.isInfinite(point.getPotential()))) {
			return; // no nothing
		}

		// check whether they are in the frozen set. only if they are, we can
		// continue.
		// if(this.frozenPoints.contains(points.first()) &&
		// this.frozenPoints.contains(points.last()))
		{
			// see: Sethian, Level Set Methods and Fast Marching Methods, page
			// 124.
			double u = p2.getPotential() - p1.getX();
			double a = p2.distance(point);
			double b = p1.distance(point);
			double c = p1.distance(p2);
			double TA = p1.getPotential();
			double TB = p2.getPotential();

			double phi = GeometryUtils.angle(p1, point, p2);
			double cosphi = Math.cos(phi);

			double F = 1.0 / this.timeCostFunction.costAt(point);

			// solve x2 t^2 + x1 t + x0 == 0
			double x2 = a * a + b * b - 2 * a * b * cosphi;
			double x1 = 2 * b * u * (a * cosphi - b);
			double x0 = b * b
					* (u * u - F * F * a * a * Math.sin(phi) * Math.sin(phi));
			double t = solveQuadratic(x2, x1, x0);

			double inTriangle = (b * (t - u) / t);
			if (u < t && a * cosphi < inTriangle && inTriangle < a / cosphi) {
				point.setPotential(Math.min(point.getPotential(), t + TA));
			} else {
				point.setPotential(Math.min(Math.min(point.getPotential(), b * F + TA), c * F + TB));
			}
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
		double det = b * b - 4 * a * c;
		if (det < 0) {
			return Double.MIN_VALUE;
		}

		return Math.max((-b + Math.sqrt(det)) / (2 * a), (-b - Math.sqrt(det))
				/ (2 * a));
	}
}
