package org.vadere.util.potential.calculators;

import java.awt.Point;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.MathUtil;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.timecost.ITimeCostFunction;

/**
 * EikonalSolverSFMM is almost identical to EikonalSolverFMM avoiding
 * the update of values inside the priority queue. Instead the queue contains
 * duplicates.
 *
 * See: jones-2006 (3D distance fields: a survey of techniques and applications)
 * See: gomez-2015 (Fast Methods for Eikonal Equations: an Experimental Survey)
 *
 */
public class EikonalSolverSFMM implements EikonalSolver {
	protected final PriorityQueue<Pair<Point, Double>> narrowBand;
	protected final ITimeCostFunction timeCostFunction;

	protected CellGrid cellGrid;
	protected List<Point> targetPoints;
	protected Collection<VShape> targetShapes;
	boolean isHighAccuracy = false;

	/** only for logging */
	protected static Logger logger = LogManager.getLogger(EikonalSolverFMM.class);
	protected long runtime = 0;

	/**
	 * Initializes the FM potential calculator with a time cost function F > 0.
	 */
	public EikonalSolverSFMM(
							final CellGrid potentialField,
	                        final Collection<VShape> targetShapes,
	                        final boolean isHighAccuracy,
	                        final ITimeCostFunction timeCostFunction) {
		this.cellGrid = potentialField;
		this.targetPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Target)
				.collect(Collectors.toList());
		this.targetShapes = targetShapes;
		this.isHighAccuracy = isHighAccuracy;

		ComparatorPotentialFieldValueSFMM comparator = new ComparatorPotentialFieldValueSFMM();
		this.narrowBand = new PriorityQueue<>(50, comparator);
		this.timeCostFunction = timeCostFunction;

		if (targetPoints.size() == 0) {
			logger.error("PotentialFieldInitializerFastMarching::Run(): "
					+ "Warning, no target points given. Target missing or grid resolution too low.");
			return;
		}
	}

	@Override
	public void initialize() {
		for (Point point : targetPoints) {
			setTargetNeighborsDistances(point);
		}

		/**
		 * Create whole Floor Field at the beginning.
		 */
		while (!narrowBand.isEmpty()) {
			Pair<Point, Double> pair = narrowBand.poll();

			Point tmpPoint = pair.getKey();
			double value = pair.getValue();

			if(value <= cellGrid.getValue(tmpPoint).potential) {
				cellGrid.getValue(tmpPoint).tag = PathFindingTag.Reached;
				setNeighborDistances(tmpPoint);
			}
		}
	}

	/**
	 * Method to support calculation on demand (to improve the performance).
	 *
	 * @param point
	 */
	private void furtherRun(final Point point) {
		while (!narrowBand.isEmpty() && cellGrid.getValue(point).tag == PathFindingTag.Undefined) {
			Pair<Point, Double> pair = narrowBand.poll();
			Point tmpPoint = pair.getKey();
			double value = pair.getValue();

			// this might be an old value
			if(value <= cellGrid.getValue(tmpPoint).potential) {
				cellGrid.getValue(tmpPoint).tag = PathFindingTag.Reached;
				setNeighborDistances(tmpPoint);
			}
		}
	}

	@Override
	public void update() {
		// logger.info("other-runtime: " + (System.currentTimeMillis() - runtime));
		long ms = System.currentTimeMillis();
		if (needsUpdate()) {
			timeCostFunction.update();
			resetDynamicPotentialField();
			initialize();
		}
		// logger.info("ffm-runtime: " + (System.currentTimeMillis() - ms));
		runtime = System.currentTimeMillis();
	}

	protected void resetDynamicPotentialField() {
		for (CellState data : cellGrid.getRawBuffer()) {
			data.potential = Double.MAX_VALUE;

			if (data.tag == PathFindingTag.Reached) {
				data.tag = PathFindingTag.Undefined;
			} else if (data.tag == PathFindingTag.Target) {
				data.potential = 0.0;
			}
		}
	}

	@Override
	public ITimeCostFunction getTimeCostFunction() {
		return timeCostFunction;
	}

	@Override
	public boolean isHighAccuracy() {
		return isHighAccuracy;
	}

	@Override
	public boolean needsUpdate() {
		return timeCostFunction.needsUpdate();
	}

	@Override
	public CellGrid getPotentialField() {
		return cellGrid;
	}

	protected void setNeighborDistances(final Point point) {
		double distance;

		List<Point> neighbors = MathUtil.getRelativeNeumannNeighborhood();

		for (Point neighbor : neighbors) {
			int x = point.x + neighbor.x;
			int y = point.y + neighbor.y;

			if (cellGrid.isValidPoint(x, y)) {
				if (cellGrid.getValue(x, y).tag == PathFindingTag.Undefined) {
					distance = computeGodunovDifference(x, y, cellGrid);
					cellGrid.setValue(x, y, new CellState(distance,
							PathFindingTag.Reachable));
					narrowBand.add(Pair.create(new Point(x, y), cellGrid.getValue(x, y).potential));
				} else if (cellGrid.getValue(x, y).tag == PathFindingTag.Reachable) {
					distance = computeGodunovDifference(x, y, cellGrid);

					if (distance < cellGrid.getValue(x, y).potential) {
						//narrowBand.remove(neighbor);
						cellGrid.getValue(x, y).potential = distance;
						narrowBand.add(Pair.create(new Point(x, y), cellGrid.getValue(x, y).potential));
					}
				}
			}
		}
	}

	protected void setTargetNeighborsDistances(final Point point) {
		List<Point> neighbors = MathUtil.getRelativeNeumannNeighborhood();

		for (Point neighbor : neighbors) {
			int x = point.x + neighbor.x;
			int y = point.y + neighbor.y;

			if (cellGrid.isValidPoint(x, y) && cellGrid.getValue(x, y).tag == PathFindingTag.Undefined) {
				cellGrid.setValue(
						x, y,
						new CellState(minDistanceToTarget(cellGrid.pointToCoord(x, y)),
								PathFindingTag.Reachable));
				narrowBand.add(Pair.create(new Point(x, y), cellGrid.getValue(x, y).potential));
			}
		}
	}

	private double minDistanceToTarget(final VPoint point, VPoint dp) {
		double minDistance = Double.MAX_VALUE;
		double tmp;
		for (VShape targetShape : targetShapes) {
			// negative distances are possible when point is inside the target
			tmp = Math.max(0, targetShape.distance(point.add(dp)));
			if (tmp < minDistance) {
				minDistance = tmp;
			}
		}
		return minDistance;
	}

	private double minDistanceToTargetCentered(final VPoint point) {
		return minDistanceToTarget(point, new VPoint(cellGrid.getWidth() / (cellGrid.getNumPointsX() - 1) / 2.0,
				cellGrid.getHeight() / (cellGrid.getNumPointsY() - 1) / 2.0));
	}

	private double minDistanceToTarget(final VPoint point) {
		return minDistanceToTarget(point, new VPoint(0,0));
	}

	private class ComparatorPotentialFieldValueSFMM implements Comparator<Pair<Point, Double>> {
		@Override
		public int compare(final Pair<Point, Double> o1, final Pair<Point, Double> o2) {
			return Double.compare(o1.getValue(), o2.getValue());
		}
	}
}
