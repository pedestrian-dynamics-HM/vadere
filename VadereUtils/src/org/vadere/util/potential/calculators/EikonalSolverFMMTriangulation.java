package org.vadere.util.potential.calculators;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.data.HalfEdge;
import org.vadere.util.geometry.data.Triangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.PathFindingTag;
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
	private PriorityQueue<FFMHalfEdge> narrowBand;
	private final Collection<HalfEdge<PotentialPoint>> targetPoints;

	private Comparator<FFMHalfEdge> pointComparator = (he1, he2) -> {
		if (he1.halfEdge.getEnd().getPotential() < he2.halfEdge.getEnd().getPotential()) {
			return -1;
		} else if(he1.halfEdge.getEnd().getPotential() > he2.halfEdge.getEnd().getPotential()) {
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
			halfEdge.getEnd().setPathFindingTag(PathFindingTag.Target);
			halfEdge.getEnd().setPotential(0.0);
			narrowBand.add(new FFMHalfEdge(halfEdge));
		}

		calculate();
	}

    @Override
    public CellGrid getPotentialField() {
        return null;
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
	private void calculate() {
		if (!calculationFinished) {
			while (this.narrowBand.size() > 0) {
				System.out.println(narrowBand.size());
				// poll the point with lowest data value
				FFMHalfEdge ffmHalfEdge = this.narrowBand.poll();
				// add it to the frozen points
				ffmHalfEdge.halfEdge.getEnd().setPathFindingTag(PathFindingTag.Reached);
				// recalculate the value based on the adjacent triangles
				double potential = recalculatePoint(ffmHalfEdge.halfEdge);
				ffmHalfEdge.halfEdge.getEnd().setPotential(Math.min(ffmHalfEdge.halfEdge.getEnd().getPotential(), potential));
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
	private void setNeighborDistances(final HalfEdge<PotentialPoint> halfEdge) {
		// remove frozen points
		Iterator<HalfEdge<PotentialPoint>> it = halfEdge.incidentVertexIterator();

		while (it.hasNext()) {
			HalfEdge<PotentialPoint> neighbour = it.next();
			if(neighbour.getEnd().getPathFindingTag() == PathFindingTag.Undefined) {
				double potential = recalculatePoint(neighbour);
				neighbour.getEnd().setPotential(potential);
				neighbour.getEnd().setPathFindingTag(PathFindingTag.Reachable);
				narrowBand.add(new FFMHalfEdge(neighbour));

			}
			else if(neighbour.getEnd().getPathFindingTag() == PathFindingTag.Reachable) {
				//double potential = neighbour.getEnd().getPotential();
				double potential = recalculatePoint(neighbour);

				// neighbour might be already in the narrowBand => update it
				if (potential < neighbour.getEnd().getPotential()) {
					FFMHalfEdge ffmHalfEdge = new FFMHalfEdge(neighbour);
					narrowBand.remove(new FFMHalfEdge(neighbour));
					neighbour.getEnd().setPotential(potential);
					narrowBand.add(ffmHalfEdge);
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
	private double recalculatePoint(final HalfEdge<PotentialPoint> point) {
		// loop over all, check whether the point is contained and update its
		// value accordingly
		double potential = Double.MAX_VALUE;
		Iterator<Face<PotentialPoint>> it = point.incidentFaceIterator();
		while (it.hasNext()) {
			Face<PotentialPoint> face = it.next();
			if(!face.isBorder()) {
				potential = Math.min(updatePoint(point, face), potential);
			}
		}
		return potential;
	}

	/**
	 * Updates a point given a triangle. The point can only be updated if the
	 * triangle contains it and the other two points are in the frozen band.
	 *
	 * @param halfEdge
	 * @param face
	 */
    private double updatePoint(final HalfEdge<PotentialPoint> halfEdge, final Face<PotentialPoint> face) {
        // check whether the triangle does contain useful data
        List<PotentialPoint> points = face.getPoints();
        points.removeIf(p -> p.equals(halfEdge.getEnd()));

        assert points.size() == 2;

		PotentialPoint p1 = points.get(0);
		PotentialPoint p2 = points.get(1);
		PotentialPoint point = halfEdge.getEnd();

		if ((Double.isInfinite(p1.getPotential()) && Double.isInfinite((p2.getPotential())))
				|| (Double.isInfinite(p1.getPotential()) && Double.isInfinite(point.getPotential()))
				|| (Double.isInfinite(p2.getPotential()) && Double.isInfinite(point.getPotential()))) {
			return halfEdge.getEnd().getPotential();
		}

		// check whether they are in the frozen set. only if they are, we can
		// continue.
		// if(this.frozenPoints.contains(points.first()) &&
		// this.frozenPoints.contains(points.last()))

		if(p1.getPathFindingTag().frozen && p2.getPathFindingTag().frozen)
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
				return t + TA;
			} else {
				return Math.min(b * F + TA, c * F + TB);
			}
		}
		else {
			return halfEdge.getEnd().getPotential();
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

	/**
	 * We require a half-edge that has an equals which only depends on the end-vertex.
	 */
	private class FFMHalfEdge {
		private HalfEdge<PotentialPoint> halfEdge;

		public FFMHalfEdge(final HalfEdge<PotentialPoint> halfEdge){
			this.halfEdge = halfEdge;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			FFMHalfEdge that = (FFMHalfEdge) o;

			return halfEdge.getEnd().equals(that.halfEdge.getEnd());
		}

		@Override
		public int hashCode() {
			return halfEdge.getEnd().hashCode();
		}

		@Override
		public String toString() {
			return halfEdge.toString();
		}
	}
}
