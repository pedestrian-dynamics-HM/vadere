package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.utils.cache.CacheException;
import org.vadere.simulator.utils.cache.ICacheObject;
import org.vadere.simulator.utils.cache.ICellGridCacheObject;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.MathUtil;

import java.awt.*;

/**
 * @author Benedikt Zoennchen
 */
public interface GridEikonalSolver extends EikonalSolver {

	CellGrid getCellGrid();

	// TODO: implement this!
	default IMesh<?, ?, ?> getDiscretization(final CellGrid potentialField) {
		return new PMesh();
	}

	@Override
	default IMesh<?, ?, ?> getDiscretization() {
		return getDiscretization(getCellGrid());
	}

	default double getPotential(final CellGrid potentialField, final double x, final double y, final double unknownPenalty, final double weight) {
		double targetPotential = Double.MAX_VALUE;

		/* Interpolate the known (potential < Double.MAX_VALUE) values. */
		Pair<Double, Double> result = potentialField.getInterpolatedValueAt(x, y);

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

	default double getPotential(final double x, final double y, final double unknownPenalty, final double weight) {
		return getPotential(getCellGrid(), x, y, unknownPenalty, weight);
	}

	default double getPotential(final CellGrid potentialField, final IPoint pos, final double unknownPenalty, final double weight) {
		return getPotential(potentialField, pos.getX(), pos.getY(), unknownPenalty, weight);
	}

	default double getPotential(final IPoint pos, final double unknownPenalty, final double weight) {
		return getPotential(getCellGrid(), pos.getX(), pos.getY(), unknownPenalty, weight);
	}

	default Direction computeDirection(final Point point, final CellGrid cellGrid) {
		boolean posX = false;
		boolean posY = false;
		if (isValidPoint(cellGrid, new Point(point.x + 1, point.y)) &&
				(!isValidPoint(cellGrid, new Point(point.x - 1, point.y))
						|| (cellGrid.getValue(new Point(point.x + 1, point.y)).potential < cellGrid
						.getValue(new Point(point.x - 1, point.y)).potential))) {
			posX = true;
		}

		if (isValidPoint(cellGrid, new Point(point.x, point.y + 1)) &&
				(!isValidPoint(cellGrid, new Point(point.x, point.y - 1))
						|| (cellGrid.getValue(new Point(point.x, point.y + 1)).potential < cellGrid
						.getValue(new Point(point.x, point.y - 1)).potential))) {
			posY = true;
		}

		if(posX && posY) {
			return Direction.UP_RIGHT;
		} else if(posX && !posY) {
			return Direction.BOTTOM_RIGHT;
		} else if(!posX && posY) {
			return Direction.UP_LEFT;
		} else {
			return Direction.BOTTOM_LEFT;
		}
	}

	default Point[] getPointByDirection(final Point point, final CellGrid cellGrid, final Direction direction) {
		Point xPoint, yPoint;

		switch (direction) {
			case UP_LEFT:
				xPoint = new Point(point.x - 1, point.y);
				yPoint = new Point(point.x, point.y + 1);
				break;
			case UP_RIGHT:
				xPoint = new Point(point.x + 1, point.y);
				yPoint = new Point(point.x, point.y + 1);
				break;
			case BOTTOM_LEFT:
				xPoint = new Point(point.x - 1, point.y);
				yPoint = new Point(point.x, point.y - 1);
				break;
			default:
				xPoint = new Point(point.x + 1, point.y);
				yPoint = new Point(point.x, point.y - 1);
				break;
		}
		Point[] result = new Point[2];

		double xVal = Double.MAX_VALUE;
		double yVal = Double.MAX_VALUE;
		if(isValidPoint(cellGrid, xPoint)) {
			result[0] = xPoint;
			xVal = cellGrid.getValue(xPoint).potential;
		}

		if(isValidPoint(cellGrid, yPoint)) {
			result[1] = yPoint;
			yVal = cellGrid.getValue(yPoint).potential;
		}

		double cost = getTimeCostFunction().costAt(new VPoint(point.x, point.y));
		double speed = (1.0 / cellGrid.getResolution()) / cost; // = F/cost
		double distance = 1.0 / speed;
		if(Math.abs(xVal - yVal) >= distance) {
			if(xVal > yVal) {
				result[0] = null;
			} else {
				result[1] = null;
			}
		}

		return result;
	}

	default double computeGodunovDifference(final Point point, final CellGrid cellGrid, final Direction direction) {

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
				if (isValidPoint(cellGrid, new Point(point.x + 1, point.y)) &&
						(!isValidPoint(cellGrid, new Point(point.x - 1, point.y))
								|| (cellGrid.getValue(new Point(point.x + 1, point.y)).potential < cellGrid
								.getValue(new Point(point.x - 1, point.y)).potential))) {
					xPoint = new Point(point.x + 1, point.y);
					xhPoint = new Point(point.x + 2, point.y);
				} else {
					xPoint = new Point(point.x - 1, point.y);
					xhPoint = new Point(point.x - 2, point.y);
				}

				if (isValidPoint(cellGrid, new Point(point.x, point.y + 1)) &&
						(!isValidPoint(cellGrid, new Point(point.x, point.y - 1))
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
		if (isValidPoint(cellGrid, xPoint)) {
			xVal = cellGrid.getValue(xPoint).potential;
			if (xVal != Double.MAX_VALUE) {
				a += 1.0;
				b -= 2 * xVal;
				c += Math.pow(xVal, 2);
			}
		}

		double yVal = Double.MAX_VALUE;
		if (isValidPoint(cellGrid, yPoint)) {
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
				if (isValidPoint(cellGrid, xhPoint) && cellGrid.getValue(xhPoint).potential < xVal) {
					double tp = (1.0 / 3.0) * (4.0 * xVal - cellGrid.getValue(xhPoint).potential);
					double factor = 9.0 / 4.0;
					a += factor;
					b -= 2.0 * 9.0 / 4.0 * tp;
					c += factor * Math.pow(tp, 2);
				}

				if (isValidPoint(cellGrid, yhPoint) && cellGrid.getValue(yhPoint).potential < yVal) {
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

	default Triple<Double, Point, Point> computeGodunovDifferenceAndDep(final Point point, final CellGrid cellGrid, final Direction direction) {

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
				if (isValidPoint(cellGrid, new Point(point.x + 1, point.y)) &&
						(!isValidPoint(cellGrid, new Point(point.x - 1, point.y))
								|| (cellGrid.getValue(new Point(point.x + 1, point.y)).potential < cellGrid
								.getValue(new Point(point.x - 1, point.y)).potential))) {
					xPoint = new Point(point.x + 1, point.y);
					xhPoint = new Point(point.x + 2, point.y);
				} else {
					xPoint = new Point(point.x - 1, point.y);
					xhPoint = new Point(point.x - 2, point.y);
				}

				if (isValidPoint(cellGrid, new Point(point.x, point.y + 1)) &&
						(!isValidPoint(cellGrid, new Point(point.x, point.y - 1))
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
		if (isValidPoint(cellGrid, xPoint)) {
			xVal = cellGrid.getValue(xPoint).potential;
			if (xVal != Double.MAX_VALUE) {
				a += 1.0;
				b -= 2 * xVal;
				c += Math.pow(xVal, 2);
			} else {
				xPoint = null;
			}
		} else {
			xPoint = null;
		}

		double yVal = Double.MAX_VALUE;
		if (isValidPoint(cellGrid, yPoint)) {
			yVal = cellGrid.getValue(yPoint).potential;
			if (yVal != Double.MAX_VALUE) {
				a += 1.0;
				b -= 2 * yVal;
				c += Math.pow(yVal, 2);
			} else {
				yPoint = null;
			}
		} else {
			yPoint = null;
		}

		if ((xVal != Double.MAX_VALUE ^ yVal != Double.MAX_VALUE) || Math.abs(xVal - yVal) >= distance) {
			if(xVal < yVal) {
				return Triple.of(xVal + distance, xPoint, null);
			}
			else {
				return Triple.of(yVal + distance, null, yPoint);
			}
		} else if ((xVal == Double.MAX_VALUE && yVal == Double.MAX_VALUE)) {
			// logger.warn("no solution possible");
			return Triple.of(result, null, null);
		} else {
			if (isHighAccuracy()) {
				if (isValidPoint(cellGrid, xhPoint) && cellGrid.getValue(xhPoint).potential < xVal) {
					double tp = (1.0 / 3.0) * (4.0 * xVal - cellGrid.getValue(xhPoint).potential);
					double factor = 9.0 / 4.0;
					a += factor;
					b -= 2.0 * 9.0 / 4.0 * tp;
					c += factor * Math.pow(tp, 2);
				}

				if (isValidPoint(cellGrid, yhPoint) && cellGrid.getValue(yhPoint).potential < yVal) {
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

			if(result <= xVal) {
				xPoint = null;
			}

			if(result <= yVal) {
				yPoint = null;
			}
			return Triple.of(result, xPoint, yPoint);
		}
	}

	default double computeGodunovDifference(final Point point, final Direction direction) {
		return computeGodunovDifference(point, getCellGrid(), direction);
	}

	default double computeGodunovDifference(final Point point, final CellGrid cellGrid) {
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

				if (isValidPoint(cellGrid, pni) && cellGrid.getValue(pni).tag.frozen) {
					double val1n = cellGrid.getValue(pni).potential;

					if (val1n < val1) {
						val1 = val1n;

						if (isValidPoint(cellGrid, pni2)) {
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

	default double computeGodunovDifference(final Point point) {
		return computeGodunovDifference(point, getCellGrid());
	}

	default boolean isValidPoint(@NotNull final CellGrid potentialField, @NotNull final Point point) {
		return potentialField.isValidPoint(point);
	}

	default boolean isValidPoint(@NotNull final Point point) {
		return isValidPoint(getCellGrid(), point);
	}

	@Override
	default boolean loadCachedFloorField(ICacheObject cacheObject) {
		// loadFromFilesystem floor field from cache. If it succeeds return true to indicate that the floor field
		// is initialized.
		boolean cacheLoaded = false;

		try{
			ICellGridCacheObject cellGridCache = (ICellGridCacheObject) cacheObject;
			cellGridCache.initializeObjectFromCache(getCellGrid());
			cacheLoaded = true;
		} catch (CacheException e){
			logger.errorf("Error loading cache. Initialize manually. " + e);
		}
		return cacheLoaded;
	}

	@Override
	default void saveFloorFieldToCache(ICacheObject cacheObject) {
		try{
			ICellGridCacheObject cellGridCache = (ICellGridCacheObject) cacheObject;
			cellGridCache.persistObject(getCellGrid());
		} catch (CacheException e){
			logger.errorf("Error saving cache.", e);
		}
	}
}
