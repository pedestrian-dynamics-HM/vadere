package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.MathUtil;

import java.awt.*;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * EikonalSolverFMM initializes a potential field on basis
 * of the fast marching algorithm. The potential field is static and therefor
 * not updated by update() (see PotentialFieldInitializerFastMarchingAdaptive).
 * Hence, the initializer may be used to realize static floor fields.
 * 
 */
public class EikonalSolverFMM extends AGridEikonalSolver {
	protected final PriorityQueue<Point> narrowBand;
	protected final ITimeCostFunction timeCostFunction;

	protected CellGrid cellGrid;
	protected List<Point> targetPoints;
	protected IDistanceFunction distFunc;

	boolean isHighAccuracy = false;

	/** only for logging */
	protected static Logger logger = Logger.getLogger(EikonalSolverFMM.class);
	protected long runtime = 0;
	private int updates = 0;

    /**
     * Initializes the FM potential calculator with a time cost function F > 0.
     */
    public EikonalSolverFMM(
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
        this.narrowBand = new PriorityQueue<>(50, new ComparatorPotentialFieldValue(potentialField));
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
		// TODO [priority=low] [task=feature] remove this part to enable on demand computation from the
		// beginning.
		// Be aware that in this case, the function "getValue" MUST be called,
		// it is not possible to work with the cellGrid directly.
		long ms = System.currentTimeMillis();
		while (!narrowBand.isEmpty()) {
			Point tmpPoint = narrowBand.poll();
			cellGrid.getValue(tmpPoint).tag = PathFindingTag.Reached;
			setNeighborDistances(tmpPoint);
		}
		long runTime = System.currentTimeMillis() - ms;
		logger.debug("fmm on the gird run time = " + runTime + ", #updates = " + updates);
	}

	/**
	 * Method to support calculation on demand (to improve the performance).
	 * 
	 * @param point
	 */
	private void furtherRun(final Point point) {
		Point tmpPoint;
		while (!narrowBand.isEmpty()
				&& cellGrid.getValue(point).tag == PathFindingTag.Undefined) {
			tmpPoint = narrowBand.poll();
			cellGrid.getValue(tmpPoint).tag = PathFindingTag.Reached;
			setNeighborDistances(tmpPoint);
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
					updates++;
					cellGrid.setValue(neighbor, new CellState(distance,
							PathFindingTag.Reachable));
					narrowBand.add(neighbor);
				} else if (cellGrid.getValue(neighbor).tag == PathFindingTag.Reachable) {
					distance = computeGodunovDifference(neighbor, cellGrid);
					updates++;
					if (distance < cellGrid.getValue(neighbor).potential) {
						narrowBand.remove(neighbor);
						cellGrid.getValue(neighbor).potential = distance;
						narrowBand.add(neighbor);
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
				narrowBand.add(neighbor);
			}
		}
	}
}
