package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the sequential fast sweeping method (FSM) presented in
 * 'A fast sweeping method for Eikonal equations' zhao-2005.
 *
 * The fast sweeping method uses (in 2D) predefined 4 sweeping directions (up left, up right, top
 * left, top right).
 * In each iteration the travelling time approximation will be calculated according to each sweeping
 * direction.
 * The each sweeping direction can be done in parallel. For a obstacle free topography and a
 * constant speed function F
 * and a point target, one iteration (4 sweeps) are enough to compute T.
 *
 *
 */
public class EikonalSolverFSM extends AGridEikonalSolver {
	private final CellGrid cellGrid;
	private static Logger logger = Logger.getLogger(EikonalSolverFSM.class);
	private final ITimeCostFunction timeCostFunction;
	private boolean isHighAccuracy;
	private List<Point> targetPoints;
	private final IDistanceFunction distFunc;
	private static final double EPSILON = 0.001;

	public EikonalSolverFSM(
			final CellGrid cellGrid,
			final IDistanceFunction distFunc,
			final boolean isHighAccuracy,
			final ITimeCostFunction timeCostFunction,
			final double unknownPenalty,
			final double weight) {
	    super(cellGrid, unknownPenalty, weight);
	    this.distFunc = distFunc;
		this.timeCostFunction = timeCostFunction;
		this.isHighAccuracy = isHighAccuracy;
		this.targetPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Target).collect(Collectors.toList());
		this.cellGrid = cellGrid;

		if (targetPoints.size() == 0) {
			logger.error("PotentialFieldInitializerFastMarching::Run(): "
					+ "Warning, no target points given. Target missing or grid resolution too low.");
			return;
		}
	}

	@Override
	public void solve() {
		init();
		loop();

		/*
		 * try {
		 * BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./potential.csv")));
		 * for(int row = 0; row < cellGrid.getNumPointsY(); row++) {
		 * for(int col = 0; col < cellGrid.getNumPointsX(); col++) {
		 * bw.write(cellGrid.getValue(col, row).potential+" ");
		 * }
		 * bw.write("\n");
		 * }
		 * bw.close();
		 * 
		 * } catch (IOException e) {
		 * e.printStackTrace();
		 * }
		 */
	}

	private void init() {
		// set distances of the target neighbor points
		targetPoints.stream()
				.flatMap(p -> cellGrid.getLegitNeumannNeighborhood(p).stream())
				.filter(neighbor -> cellGrid.getValue(neighbor).tag != PathFindingTag.Obstacle)
				.forEach(neighbor -> {
					cellGrid.setValue(neighbor, new CellState(Math.max(0, -distFunc.apply(cellGrid.pointToCoord(neighbor))), PathFindingTag.NARROW));
				});
	}

	private void loop() {
		int iterations = 0;
		boolean allFrozen = false;
		int itNumb = 1;

		while (!allFrozen) {

			itNumb--;
			allFrozen = true;
			iterations++;
			logger.info("iteration number: " + iterations);

			// first sweep
			for (int x = 0; x < cellGrid.getNumPointsX(); x++) {
				for (int y = 0; y < cellGrid.getNumPointsY(); y++) {
					Point point = new Point(x, y);
					if (isRelevant(point)) {
						double p = cellGrid.getValue(point).potential;
						double q = Math.min(computeGodunovDifference(point, cellGrid, Direction.ANY), p);
						cellGrid.getValue(point).potential = q;

						if (Math.abs(q - p) > EPSILON) {
							allFrozen = false;
						}
					}
				}
			}

			// second sweep
			for (int x = cellGrid.getNumPointsX() - 1; x >= 0; x--) {
				for (int y = 0; y < cellGrid.getNumPointsY(); y++) {
					Point point = new Point(x, y);
					if (isRelevant(point)) {
						double p = cellGrid.getValue(point).potential;
						double q = Math.min(computeGodunovDifference(point, cellGrid, Direction.ANY), p);
						cellGrid.getValue(point).potential = q;

						if (Math.abs(q - p) > EPSILON) {
							allFrozen = false;
						}
					}
				}
			}

			// third sweep
			for (int y = cellGrid.getNumPointsY() - 1; y >= 0; y--) {
				for (int x = cellGrid.getNumPointsX() - 1; x >= 0; x--) {
					Point point = new Point(x, y);
					if (isRelevant(point)) {
						double p = cellGrid.getValue(point).potential;
						double q = Math.min(computeGodunovDifference(point, cellGrid, Direction.ANY), p);
						cellGrid.getValue(point).potential = q;

						if (Math.abs(q - p) > EPSILON) {
							allFrozen = false;
						}
					}
				}
			}

			// fourth sweep
			for (int y = cellGrid.getNumPointsY() - 1; y >= 0; y--) {
				for (int x = 0; x < cellGrid.getNumPointsX(); x++) {
					Point point = new Point(x, y);
					if (isRelevant(point)) {
						double p = cellGrid.getValue(point).potential;
						double q = Math.min(computeGodunovDifference(point, cellGrid, Direction.ANY), p);
						cellGrid.getValue(point).potential = q;

						if (Math.abs(q - p) > EPSILON) {
							allFrozen = false;
						}
					}
				}
			}
		}
	}

	private boolean converged(final double oldValue, final double newValue) {
		double diff = Math.abs(oldValue-newValue);
		return diff < oldValue * EPSILON;
	}

	private boolean isRelevant(final Point point) {
		return cellGrid.getValue(point).tag != PathFindingTag.Target && cellGrid.getValue(point).tag != PathFindingTag.Obstacle;
	}

	@Override
	public ITimeCostFunction getTimeCostFunction() {
		return timeCostFunction;
	}

	@Override
	public boolean isHighAccuracy() {
		return false;
	}
}
