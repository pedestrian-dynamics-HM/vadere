package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import java.awt.Point;
import java.util.Comparator;

import org.vadere.util.data.cellgrid.CellGrid;

/**
 * Compares potentials of two points on a given grid returning either -1 or 1.
 * This comparison is used to order points by its respective potential value for
 * the fast marching algorithm.
 */
class ComparatorPotentialFieldValue implements Comparator<Point> {

	/** Grid whose potential have to be compared. */
	private final CellGrid grid;

	/** Creates a new comparator object for the given grid. */
	ComparatorPotentialFieldValue(CellGrid grid) {
		this.grid = grid;
	}

	/**
	 * Compares potentials of given points. Returns -1 if potential of p1 <
	 * potential of p2. Returns 1 if potential of p1 > potential of p2.
	 * 
	 * The behavior described in the following is a workaround for an
	 * point-order-dependent behavior of 'high accuracy fast marching' which is
	 * described below. If potential of poth points are equal: Returns -1 if
	 * p1.x < p2.x Returns 1 if p1.x > p2.x Returns -1 if p1.y < p2.y Returns 1
	 * if p1.y > p2.y, otherwise 0.
	 * 
	 * Generally, the first mentioned comparisons should be sufficient to
	 * retrieve reproducible results by the fast marching algorithm. One may
	 * expect, that the fast marching algorithm is not influenced by the order
	 * of points whose potentials are equal. However, the 'high accuracy fast
	 * marching' produces asymmetric results, even if the problem is symmetric.
	 * The reason for that may be the switch between using the first order
	 * derivate and the second order derivate (see fast marching algorithm for
	 * more information). These asymmetric results are influenced by the order
	 * of points with same potentials. To ensure at least a reproducible result
	 * the position of the points is also taken into account for comparison.
	 * 
	 * If one would return 0 for points with same potential, the order of these
	 * points is determined by some pseudo-random characteristics like location
	 * of the variable in memory. This leads to different results between two
	 * runs even on the same machine.
	 */
	@Override
	public int compare(Point p1, Point p2) {
		// return (int)Math.signum( grid.getValue( p1 ).potential -
		// grid.getValue( p2 ).potential );
		/* Return -1 if p1.pot < p2.pot. */
		if (grid.getValue(p1).potential < grid.getValue(p2).potential) {
			return -1;
		} else if (grid.getValue(p1).potential > grid.getValue(p2).potential) {
			return 1;
		}

		/* Otherwise use point coordinates as criteria for sorting. */
		if (p1.x < p2.x) {
			return -1;
		}

		if (p1.x > p2.x) {
			return 1;
		}

		if (p1.y < p2.y) {
			return -1;
		}

		if (p1.y > p2.y) {
			return 1;
		}

		/* Return 0 if location of points is equal. */
		return 0;
	}
}
