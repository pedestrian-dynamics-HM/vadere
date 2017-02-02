package org.vadere.util.potential.calculators;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.math.MathUtil;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.timecost.ITimeCostFunction;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;

import java.util.*;
import java.awt.Point;
import java.util.function.Function;

/**
 * The eikonal solver, solves the eikonal equation on a Cartesian grid. In case of a changing F the
 * solver re-computes the solution if update is called.
 */
public interface EikonalSolver {

	Logger logger = LogManager.getLogger(EikonalSolver.class);

	enum Direction {
		UP_LEFT, UP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, ANY;
	}

	/**
	 * Computes potentials on basis of the given data. Should be called from
	 * outside only once for initialization.
	 */
	void initialize();

	/**
	 * Recomputes the potentials. May be called every simulation step. May
	 * contain an empty implementation for static floor field initializers.
	 */
	default void update() {}

	/**
	 * Returns true if the potential field needs an update. The value indicates
	 * the type of initializer: static or dynamic floor field. Initializer of
	 * static floor fields may return false, initializers of dynamic floor
	 * fields may return true.
	 */
	default boolean needsUpdate() {
		return false;
	}

	/**
	 *
	 * @param point
	 * @return
	 */
	default double getValue(final Point point, final CellGrid cellGrid) {
		return cellGrid.getValue(point).potential;
	}

	CellGrid getPotentialField();

	double getValue(final double x, final double y);

	default double getValue(final IPoint position) {
		return getValue(position.getX(), position.getY());
	}

    /**
     * Returns a copy of the current (for the current F which might change over time) solution of the eikonal equation.
     *
     * @return a copy of the current solution of the eikonal equation
     */
    default Function<VPoint, Double> getSolution(final double unknownPenalty, final double weight) {
        // copy the current data
        final CellGrid potentialField = getPotentialField().clone();
        return p -> getPotential(potentialField, p, unknownPenalty, weight);
    }

    default double getPotential(final VPoint pos, final double unknownPenalty, final double weight) {
        return getPotential(getPotentialField(), pos, unknownPenalty, weight);
    }

    default double getPotential(final CellGrid potentialField, final VPoint pos, final double unknownPenalty, final double weight) {
        double targetPotential = Double.MAX_VALUE;
        Point gridPoint = potentialField.getNearestPointTowardsOrigin(pos);
        VPoint gridPointCoord = potentialField.pointToCoord(gridPoint);
        int incX = 1, incY = 1;
        double gridPotentials[];

        if (pos.x >= potentialField.getWidth()) {
            incX = 0;
        }

        if (pos.y >= potentialField.getHeight()) {
            incY = 0;
        }

        java.util.List<Point> points = new LinkedList<>();
        points.add(gridPoint);
        points.add(new Point(gridPoint.x + incX, gridPoint.y));
        points.add(new Point(gridPoint.x + incX, gridPoint.y + incY));
        points.add(new Point(gridPoint.x, gridPoint.y + incY));
        gridPotentials = getGridPotentials(points, potentialField);

		/* Interpolate the known (potential < Double.MAX_VALUE) values. */
        Pair<Double, Double> result = InterpolationUtil.bilinearInterpolationWithUnkown(
                gridPotentials,
                (pos.x - gridPointCoord.x) / potentialField.getResolution(),
                (pos.y - gridPointCoord.y) / potentialField.getResolution());

        double tmpPotential = result.getLeft();

        // weightOfKnown is in (0,1)
        double weightOfKnown = result.getRight();

		/*
		 * If at least one node is known, a specialized version of
		 * interpolation is used: If the divisor weightOfKnown[ 0 ] would
		 * not be part of the equation, it would be a general bilinear
		 * interpolation using obstacleGridPenalty for the unknown. However,
		 * as soon as the interpolated value is not on the line of known
		 * values (weightOfKnown < 1) the potential is increased, like an
		 * additional penalty. The more the interpolated value moves into
		 * direction of the unknown, the higher the penalty becomes.
		 * The values are only unknown at the boundary!
		 */
        if (weightOfKnown > 0.00001) {
            tmpPotential = tmpPotential / weightOfKnown
                    + (1 - weightOfKnown)
                    * unknownPenalty;
        } else /* If all values are maximal, set potential to maximum. */
        {
            tmpPotential = Double.MAX_VALUE;
        }

        tmpPotential *= weight;

        if (tmpPotential < targetPotential) {
            targetPotential = tmpPotential;
        }


        return targetPotential;
    }

    default double[] getGridPotentials(final java.util.List<Point> points, final CellGrid potentialField) {
        double[] result = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            result[i] = potentialField.getValue(points.get(i)).potential;
        }

        return result;
    }


	default boolean isHighAccuracy() {
		return true;
	}

	default ITimeCostFunction getTimeCostFunction() {
		return new UnitTimeCostFunction();
	}

}
