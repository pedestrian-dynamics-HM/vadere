package org.vadere.util.potential.calculators;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.potential.CellGrid;

import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class AbstractGridEikonalSolver implements EikonalSolver {
	protected CellGrid potentialField;
	private double unknownPenalty;

	public AbstractGridEikonalSolver(final CellGrid potentialField, final double unknownPenalty) {
		this.potentialField = potentialField;
		this.unknownPenalty = unknownPenalty;
	}

	@Override
	public double getValue(final double x, final double y) {

		Point gridPoint = potentialField.getNearestPointTowardsOrigin(x, y);
		VPoint gridPointCoord = potentialField.pointToCoord(gridPoint);
		int incX = 1, incY = 1;
		double gridPotentials[];
		double weightOfKnown[] = new double[1];

		if (x >= potentialField.getWidth()) {
			incX = 0;
		}

		if (y >= potentialField.getHeight()) {
			incY = 0;
		}

		java.util.List<Point> points = new LinkedList<>();
		points.add(gridPoint);
		points.add(new Point(gridPoint.x + incX, gridPoint.y));
		points.add(new Point(gridPoint.x + incX, gridPoint.y + incY));
		points.add(new Point(gridPoint.x, gridPoint.y + incY));
		gridPotentials = getGridPotentials(points);

				/* Interpolate the known (potential < Double.MAX_VALUE) values. */
		double tmpPotential =  InterpolationUtil.bilinearInterpolationWithUnknown(gridPotentials,
				(x - gridPointCoord.x)
						/ potentialField.getResolution(),
				(y - gridPointCoord.y)
						/ potentialField.getResolution(),
				weightOfKnown);
		/*
		 * If at least one node is known, a specialized version of
		 * interpolation is used: If the divisor weightOfKnown[ 0 ] would
		 * not be part of the equation, it would be a general bilinear
		 * interpolation using obstacleGridPenalty for the unknown. However,
		 * as soon as the interpolated value is not on the line of known
		 * values (weightOfKnown < 1) the potential is increased, like an
		 * additional penalty. The more the interpolated value moves into
		 * direction of the unknown, the higher the penalty becomes.
		 */
		if (weightOfKnown[0] > 0.00001) {
			tmpPotential = tmpPotential / weightOfKnown[0]
					+ (1 - weightOfKnown[0])
					* unknownPenalty;
		} else /* If all values are maximal, set potential to maximum. */
		{
			tmpPotential = Double.MAX_VALUE;
		}

		return tmpPotential;
	}

	public CellGrid getPotentialField() {
		return potentialField;
	}

	private double[] getGridPotentials(final List<Point> points) {
		double[] result = new double[points.size()];

		for (int i = 0; i < points.size(); i++) {
			result[i] = potentialField.getValue(points.get(i)).potential;
		}

		return result;
	}
}
