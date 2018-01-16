package org.vadere.util.potential.calculators.cartesian;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.math.MathUtil;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.calculators.EikonalSolver;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public abstract class AGridEikonalSolver implements EikonalSolver {
	protected CellGrid potentialField;
	private final double unknownPenalty;
	private final double weight;

	public AGridEikonalSolver(final CellGrid potentialField, final double unknownPenalty, final double weight) {
		this.potentialField = potentialField;
		this.unknownPenalty = unknownPenalty;
		this.weight = weight;
	}

    public Function<VPoint, Double> getPotentialField() {
        CellGrid clone = potentialField.clone();
        return p -> getPotential(clone, p.getX(), p.getY());
    }

    public CellGrid getCellGrid() {
	    return potentialField;
    }

    private double getPotential(final CellGrid cellGrid, final double x, final double y) {

        Point gridPoint = cellGrid.getNearestPointTowardsOrigin(x, y);
        VPoint gridPointCoord = cellGrid.pointToCoord(gridPoint);
        int incX = 1, incY = 1;
        double gridPotentials[];

        if (x >= cellGrid.getWidth() + cellGrid.getMinY()) {
            incX = 0;
        }

        if (y >= cellGrid.getHeight() + cellGrid.getMinY()) {
            incY = 0;
        }

        java.util.List<Point> points = new LinkedList<>();
        points.add(gridPoint);
        points.add(new Point(gridPoint.x + incX, gridPoint.y));
        points.add(new Point(gridPoint.x + incX, gridPoint.y + incY));
        points.add(new Point(gridPoint.x, gridPoint.y + incY));
        gridPotentials = getGridPotentials(cellGrid, points);

				/* Interpolate the known (potential < Double.MAX_VALUE) values. */
        Pair<Double, Double> result =  InterpolationUtil.bilinearInterpolationWithUnkown(gridPotentials,
                (x - gridPointCoord.x)
                        / cellGrid.getResolution(),
                (y - gridPointCoord.y)
                        / cellGrid.getResolution());

        double tmpPotential = result.getLeft();
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
		 */
        if (weightOfKnown > 0.00001) {
            tmpPotential = (tmpPotential / weightOfKnown + (1 - weightOfKnown) * unknownPenalty) * weight;
        } else /* If all values are maximal, set potential to maximum. */
        {
            tmpPotential = Double.MAX_VALUE;
        }

        return tmpPotential;
    }

    @Override
	public double getPotential(final double x, final double y) {
        return getPotential(potentialField, x, y);
	}

	public boolean isValidPoint(final Point point) {
		return potentialField.isValidPoint(point);
	}

	private double[] getGridPotentials(final CellGrid cellGrid, final List<Point> points) {
		double[] result = new double[points.size()];

		for (int i = 0; i < points.size(); i++) {
			result[i] = cellGrid.getValue(points.get(i)).potential;
		}

		return result;
	}

	protected double computeGodunovDifference(final Point point, final CellGrid cellGrid, final Direction direction) {

		VPoint position = cellGrid.pointToCoord(point.x, point.y);
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
				xPoint = new Point(point.x - 1, point.y);
				yPoint = new Point(point.x, point.y + 1);
				xhPoint = new Point(point.x - 2, point.y);
				yhPoint = new Point(point.x, point.y + 2);
				break;
			case UP_RIGHT:
				xPoint = new Point(point.x + 1, point.y);
				yPoint = new Point(point.x, point.y + 1);
				xhPoint = new Point(point.x + 2, point.y);
				yhPoint = new Point(point.x, point.y + 2);
				break;
			case BOTTOM_LEFT:
				xPoint = new Point(point.x - 1, point.y);
				yPoint = new Point(point.x, point.y - 1);
				xhPoint = new Point(point.x - 2, point.y);
				yhPoint = new Point(point.x, point.y - 2);
				break;
			case BOTTOM_RIGHT:
				xPoint = new Point(point.x + 1, point.y);
				yPoint = new Point(point.x, point.y - 1);
				xhPoint = new Point(point.x + 2, point.y);
				yhPoint = new Point(point.x, point.y - 2);
				break;
			default: {
				if (isValidPoint(new Point(point.x + 1, point.y)) &&
						(!isValidPoint(new Point(point.x - 1, point.y))
								|| (cellGrid.getValue(new Point(point.x + 1, point.y)).potential < cellGrid
								.getValue(new Point(point.x - 1, point.y)).potential))) {
					xPoint = new Point(point.x + 1, point.y);
					xhPoint = new Point(point.x + 2, point.y);
				} else {
					xPoint = new Point(point.x - 1, point.y);
					xhPoint = new Point(point.x - 2, point.y);
				}

				if (isValidPoint(new Point(point.x, point.y + 1)) &&
						(!isValidPoint(new Point(point.x, point.y - 1))
								|| (cellGrid.getValue(new Point(point.x, point.y + 1)).potential < cellGrid
								.getValue(new Point(point.x, point.y - 1)).potential))) {
					yPoint = new Point(point.x, point.y + 1);
					yhPoint = new Point(point.x, point.y + 2);
				} else {
					yPoint = new Point(point.x, point.y - 1);
					yhPoint = new Point(point.x, point.y - 2);
				}
			}
		}

		double xVal = Double.MAX_VALUE;
		if (isValidPoint(xPoint)) {
			xVal = cellGrid.getValue(xPoint).potential;
			if (xVal != Double.MAX_VALUE) {
				a += 1.0;
				b -= 2 * xVal;
				c += Math.pow(xVal, 2);
			}
		}

		double yVal = Double.MAX_VALUE;
		if (isValidPoint(yPoint)) {
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
				if (isValidPoint(xhPoint) && cellGrid.getValue(xhPoint).potential < xVal) {
					double tp = (1.0 / 3.0) * (4.0 * xVal - cellGrid.getValue(xhPoint).potential);
					double factor = 9.0 / 4.0;
					a += factor;
					b -= 2.0 * 9.0 / 4.0 * tp;
					c += factor * Math.pow(tp, 2);
				}

				if (isValidPoint(yhPoint) && cellGrid.getValue(yhPoint).potential < yVal) {
					double tp = (1.0 / 3.0) * (4.0 * yVal - cellGrid.getValue(yhPoint).potential);
					double factor = 9.0 / 4.0;
					a += factor;
					b -= 2.0 * factor * tp;
					c += factor * Math.pow(tp, 2);
				}
			}
			java.util.List<Double> solutions = MathUtil.solveQuadratic(a, b, c);
			int numberOfSolutions = solutions.size();

			if (numberOfSolutions == 2) {
				result = Math.max(solutions.get(0), solutions.get(1));
			} else if (numberOfSolutions == 1) {
				result = solutions.get(0);
			}
		}

		return result;
	}

	protected double computeGodunovDifference(final Point point, final CellGrid cellGrid) {
		double result = Double.MAX_VALUE;

		// enables cost fields with cost != 1
		VPoint position = cellGrid.pointToCoord(point.x, point.y);
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
				Point pni = new Point(point.x + neighbors.get(2 * j + i).x,
						point.y + neighbors.get(2 * j + i).y);
				Point pni2 = new Point(
						point.x + neighbors.get(2 * j + i).x * 2, point.y
						+ neighbors.get(2 * j + i).y * 2);

				if (isValidPoint(pni) && cellGrid.getValue(pni).tag.frozen) {
					double val1n = cellGrid.getValue(pni).potential;

					if (val1n < val1) {
						val1 = val1n;

						if (isValidPoint(pni2)) {
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

		java.util.List<Double> solutions = MathUtil
				.solveQuadratic(coeff2, coeff1, coeff0);
		int numberOfSolutions = solutions.size();

		if (numberOfSolutions == 2) {
			result = Math.max(solutions.get(0), solutions.get(1));
		} else if (numberOfSolutions == 1) {
			result = solutions.get(0);
		}

		return result;
	}
}
