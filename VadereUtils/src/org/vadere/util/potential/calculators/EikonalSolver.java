package org.vadere.util.potential.calculators;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.math.MathUtil;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.timecost.ITimeCostFunction;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;

import java.awt.*;
import java.awt.List;
import java.util.*;
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
	default double getValue(final Point point) {
		return getPotentialField().getValue(point).potential;
	}

	CellGrid getPotentialField();

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

		/* Interpolate the known (potential < Double.MAX_VALUE) values. */
        Pair<Double, Double> result = potentialField.getInterpolatedValueAt(pos);

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

	default boolean isValidPoint(final Point point, final CellGrid cellGrid) {
		return cellGrid.isValidPoint(point);
	}

	default boolean isValidPoint(final int x, final int y, final CellGrid cellGrid) {
		return cellGrid.isValidPoint(x, y);
	}

	default double computeGodunovDifference(final Point point, final CellGrid cellGrid, final Direction direction) {
		return computeGodunovDifference(point.x, point.y, cellGrid, direction);
	}

	default double computeGodunovDifference(final int x, int y, final CellGrid cellGrid, final Direction direction) {

		VPoint position = cellGrid.pointToCoord(x, y);
		double cost = getTimeCostFunction().costAt(new VPoint(position.x, position.y));
		double speed = (1.0 / cellGrid.getResolution()) / cost; // = F/cost

		double a = 0;
		double b = 0;
		double distance = 1.0 / speed;
		double c = -1.0 / (speed * speed);

		double result = Double.MAX_VALUE;

		Point xPoint;
		Point yPoint;

		Point xhPoint;
		Point yhPoint;
		switch (direction) {
			case UP_LEFT:
				xPoint = new Point(x - 1, y);
				yPoint = new Point(x, y + 1);
				xhPoint = new Point(x - 2, y);
				yhPoint = new Point(x, y + 2);
				break;
			case UP_RIGHT:
				xPoint = new Point(x + 1, y);
				yPoint = new Point(x, y + 1);
				xhPoint = new Point(x + 2, y);
				yhPoint = new Point(x, y + 2);
				break;
			case BOTTOM_LEFT:
				xPoint = new Point(x - 1, y);
				yPoint = new Point(x, y - 1);
				xhPoint = new Point(x - 2, y);
				yhPoint = new Point(x, y - 2);
				break;
			case BOTTOM_RIGHT:
				xPoint = new Point(x + 1, y);
				yPoint = new Point(x, y - 1);
				xhPoint = new Point(x + 2, y);
				yhPoint = new Point(x, y - 2);
				break;
			default: {
				if (isValidPoint(x + 1, y, cellGrid) &&
						(!isValidPoint(x - 1, y, cellGrid)
								|| (cellGrid.getValue(x + 1, y).potential < cellGrid
								.getValue(x - 1, y).potential))) {
					xPoint = new Point(x + 1, y);
					xhPoint = new Point(x + 2, y);
				} else {
					xPoint = new Point(x - 1, y);
					xhPoint = new Point(x - 2, y);
				}

				if (isValidPoint(x, y + 1, cellGrid) &&
						(!isValidPoint(x, y - 1, cellGrid)
								|| (cellGrid.getValue(x, y + 1).potential < cellGrid
								.getValue(x, y - 1).potential))) {
					yPoint = new Point(x, y + 1);
					yhPoint = new Point(x, y + 2);
				} else {
					yPoint = new Point(x, y - 1);
					yhPoint = new Point(x, y - 2);
				}
			}
		}

		double xVal = Double.MAX_VALUE;
		if (isValidPoint(xPoint, cellGrid)) {
			xVal = cellGrid.getValue(xPoint).potential;
			if (xVal != Double.MAX_VALUE) {
				a += 1.0;
				b -= 2 * xVal;
				c += Math.pow(xVal, 2);
			}
		}

		double yVal = Double.MAX_VALUE;
		if (isValidPoint(yPoint, cellGrid)) {
			yVal = cellGrid.getValue(yPoint).potential;
			if (yVal != Double.MAX_VALUE) {
				a += 1.0;
				b -= 2 * yVal;
				c += Math.pow(yVal, 2);
			}
		}

		if ((xVal != Double.MAX_VALUE ^ yVal != Double.MAX_VALUE) || Math.abs(xVal - yVal) >= distance) {
			result = Math.min(xVal, yVal) + distance;
		} else if ((xVal == Double.MAX_VALUE && yVal == Double.MAX_VALUE)) {
			// logger.warn("no solution possible");
		} else {
			if (isHighAccuracy()) {
				if (isValidPoint(xhPoint, cellGrid) && cellGrid.getValue(xhPoint).potential < xVal) {
					double tp = (1.0 / 3.0) * (4.0 * xVal - cellGrid.getValue(xhPoint).potential);
					double factor = 9.0 / 4.0;
					a += factor;
					b -= 2.0 * 9.0 / 4.0 * tp;
					c += factor * Math.pow(tp, 2);
				}

				if (isValidPoint(yhPoint, cellGrid) && cellGrid.getValue(yhPoint).potential < yVal) {
					double tp = (1.0 / 3.0) * (4.0 * yVal - cellGrid.getValue(yhPoint).potential);
					double factor = 9.0 / 4.0;
					a += factor;
					b -= 2.0 * factor * tp;
					c += factor * Math.pow(tp, 2);
				}
			}


			return MathUtil.solveQuadraticMax(a, b, c);
		}

		return result;
	}

	default double computeGodunovDifference(final int x, final int y, final CellGrid cellGrid) {
		// enables cost fields with cost != 1
		VPoint position = cellGrid.pointToCoord(x, y);
		double cost = getTimeCostFunction().costAt(new VPoint(position.x, position.y));

		double speed = (1.0 / cellGrid.getResolution()) / cost; // = F/cost

		double coeff0 = -1.0 / (speed * speed); // = - (F/cost)^2

		double coeff1 = 0;
		double coeff2 = 0;

		java.util.List<Point> neighbors = MathUtil.getRelativeNeumannNeighborhood();

		for (int j = 0; j < 2; j++) {
			double val1 = Double.MAX_VALUE;
			double val2 = Double.MAX_VALUE;

			for (int i = 0; i < 2; i++) {
				Point pni = new Point(x + neighbors.get(2 * j + i).x,
						y + neighbors.get(2 * j + i).y);
				Point pni2 = new Point(
						x + neighbors.get(2 * j + i).x * 2, y
						+ neighbors.get(2 * j + i).y * 2);

				if (isValidPoint(pni, cellGrid) && cellGrid.getValue(pni).tag.frozen) {
					double val1n = cellGrid.getValue(pni).potential;

					if (val1n < val1) {
						val1 = val1n;

						if (isValidPoint(pni2, cellGrid)) {
							double val2n = cellGrid.getValue(pni2).potential;
							if (cellGrid.getValue(pni2).tag.frozen
									&& val2n <= val1n) {
								val2 = val2n;
							} else {
								val2 = Double.MAX_VALUE;
							}
						}
					}
				}
			}

			if (val2 != Double.MAX_VALUE && isHighAccuracy()) {
				double tp = (1.0 / 3.0) * (4.0 * val1 - val2);
				double a = 9.0 / 4.0;
				coeff2 += a;
				coeff1 -= 2.0 * a * tp;
				coeff0 += a * Math.pow(tp, 2);
			} else if (val1 != Double.MAX_VALUE) {
				coeff2 += 1.0;
				coeff1 -= 2.0 * val1;
				coeff0 += Math.pow(val1, 2);
			}
		}

		return MathUtil.solveQuadraticMax(coeff2, coeff1, coeff0);
	}

	default double computeGodunovDifference(final Point point, final CellGrid cellGrid) {
		return computeGodunovDifference(point.x, point.y, cellGrid);
	}
}
