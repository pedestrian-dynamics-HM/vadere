package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import org.apache.commons.math3.util.Pair;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.MathUtil;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * EikonalSolverSFMM is almost identical to EikonalSolverFMM avoiding
 * the update of values inside the priority queue. Instead the queue contains
 * duplicates.
 *
 * See: jones-2006 (3D distance fields: a survey of techniques and applications)
 * See: gomez-2015 (Fast Methods for Eikonal Equations: an Experimental Survey)
 *
 */
public class EikonalSolverSFMM extends AGridEikonalSolver {
	protected final PriorityQueue<Pair<Point, Double>> narrowBand;
	protected final ITimeCostFunction timeCostFunction;

	protected CellGrid cellGrid;
	protected List<Point> targetPoints;
	protected IDistanceFunction distFunc;
	boolean isHighAccuracy = false;

	/** only for logging */
	protected static Logger logger = Logger.getLogger(EikonalSolverSFMM.class);
	protected long runtime = 0;

	/**
	 * Initializes the FM potential calculator with a time cost function F > 0.
	 */
	public EikonalSolverSFMM(
							final CellGrid potentialField,
	                        final IDistanceFunction distFunc,
	                        final boolean isHighAccuracy,
	                        final ITimeCostFunction timeCostFunction,
							final double unknownPenalty,
							final double weight) {
		super(potentialField, unknownPenalty, weight);
		this.cellGrid = potentialField;
		this.targetPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Target).collect(Collectors.toList());
		this.distFunc = distFunc;
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
	public void solve() {
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
			solve();
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

	protected void setNeighborDistances(final Point point) {
		List<Point> neighbors = MathUtil.getNeumannNeighborhood(point);
		double distance;

		for (Point neighbor : neighbors) {

			if (cellGrid.isValidPoint(neighbor)) {
				if (cellGrid.getValue(neighbor).tag == PathFindingTag.Undefined) {
					distance = computeGodunovDifference(neighbor, cellGrid);
					cellGrid.setValue(neighbor, new CellState(distance,
							PathFindingTag.Reachable));
					narrowBand.add(Pair.create(new Point(neighbor), cellGrid.getValue(neighbor).potential));
				} else if (cellGrid.getValue(neighbor).tag == PathFindingTag.Reachable) {
					distance = computeGodunovDifference(neighbor, cellGrid);

					if (distance < cellGrid.getValue(neighbor).potential) {
						narrowBand.remove(neighbor);
						cellGrid.getValue(neighbor).potential = distance;
						narrowBand.add(Pair.create(new Point(neighbor), cellGrid.getValue(neighbor).potential));
					}
				}
			}
		}
	}

	protected void setTargetNeighborsDistances(final Point point) {
		List<Point> neighbors = cellGrid.getLegitNeumannNeighborhood(point);

		for (Point neighbor : neighbors) {
			if (cellGrid.getValue(neighbor).tag == PathFindingTag.Undefined) {
				double distance = Math.max(0, -distFunc.apply(cellGrid.pointToCoord(neighbor)));
				double timeCost = timeCostFunction.costAt(cellGrid.pointToCoord(neighbor));
				double potential = distance / timeCost;
				cellGrid.setValue(neighbor, new CellState(potential, PathFindingTag.Reachable));
				narrowBand.add(Pair.create(new Point(neighbor.x, neighbor.y), cellGrid.getValue(neighbor.x, neighbor.y).potential));
			}
		}
	}

	private class ComparatorPotentialFieldValueSFMM implements Comparator<Pair<Point, Double>> {
		@Override
		public int compare(final Pair<Point, Double> o1, final Pair<Point, Double> o2) {
			return Double.compare(o1.getValue(), o2.getValue());
		}
	}
}
