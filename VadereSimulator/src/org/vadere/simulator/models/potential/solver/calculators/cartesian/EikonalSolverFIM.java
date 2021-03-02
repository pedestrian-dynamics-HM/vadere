package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;


/**
 * Implementation of the sequential fast iterative method (FIM) presented in
 * 'A Fast Iterative Method for Eikonal Equations' jeong-2008.
 *
 * The FIM essentially drops the condition that the narrow band is sorted.
 * In each step all points contained in the narrow band (which is called active list)
 * can be treated in parallel.
 *
 *
 */
public class EikonalSolverFIM extends AGridEikonalSolver {

	private final IDistanceFunction distFunc;
	private List<Point> targetPoints;
	private static Logger logger = Logger.getLogger(EikonalSolverFIM.class);
	private ITimeCostFunction timeCostFunction;
	private boolean isHighAccuracy;
	private boolean isActiveList[][];
    private final CellGrid cellGrid;
	private final double epsilon;
	private int nUpdates;

	private LinkedList<Point> activeList;

	public EikonalSolverFIM(
			final CellGrid cellGrid,
			final IDistanceFunction distFunc,
			final boolean isHighAccuracy,
			final ITimeCostFunction timeCostFunction,
            final double unknownPenalty,
            final double weight) {
	    super(cellGrid, unknownPenalty, weight);
	    this.cellGrid = cellGrid;
		this.timeCostFunction = timeCostFunction;
		this.isHighAccuracy = isHighAccuracy;
		this.distFunc = distFunc;
		this.targetPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Target).collect(Collectors.toList());
		this.activeList = new LinkedList<>();
		this.epsilon = cellGrid.getResolution() / 1000;

		if (targetPoints.size() == 0) {
			logger.error("PotentialFieldInitializerFastMarching::Run(): "
					+ "Warning, no target points given. Target missing or grid resolution too low.");
			return;
		}
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

	private void init() {
		// set distances of the target neighbor points
		resetDynamicPotentialField();
		targetPoints.stream()
				.flatMap(p -> cellGrid.getLegitNeumannNeighborhood(p).stream())
				.filter(neighbor -> cellGrid.getValue(neighbor).tag != PathFindingTag.Obstacle)
				.forEach(neighbor -> {
					if (cellGrid.getValue(neighbor).tag != PathFindingTag.NARROW) {
						activeList.add(neighbor);
					}
					cellGrid.setValue(neighbor, new CellState(Math.max(0, -distFunc.apply(cellGrid.pointToCoord(neighbor))), PathFindingTag.NARROW));
				});
	}

	private void loop() {
		int pointsLooked = 0;

		while (!activeList.isEmpty()) {

			pointsLooked += activeList.size();
			// logger.info("number of treated points: " + pointsLooked);
			// logger.info("active points: " + activeList.size());

			ListIterator<Point> activeListIterator = activeList.listIterator();
			LinkedList<Point> newActiveList = new LinkedList<>();

			while (activeListIterator.hasNext()) {
				Point activePoint = activeListIterator.next();
				double p = cellGrid.getValue(activePoint).potential;
				double q = Math.min(computeGodunovDifference(activePoint, cellGrid, Direction.ANY), p);
				cellGrid.getValue(activePoint).potential = q;

				// converged
				nUpdates++;
				if (Math.abs(p - q) <= epsilon) {
					for (Point neighbour : cellGrid.getLegitNeumannNeighborhood(activePoint)) {
						if (cellGrid.getValue(neighbour).tag != PathFindingTag.NARROW
								&& cellGrid.getValue(neighbour).tag != PathFindingTag.Obstacle) {

							double pp = cellGrid.getValue(neighbour).potential;
							double qq = computeGodunovDifference(neighbour, cellGrid, Direction.ANY);

							// converged
							if (qq < (pp - epsilon)) {
								cellGrid.setValue(neighbour, new CellState(qq, PathFindingTag.NARROW));
								newActiveList.add(neighbour);
							}
						}
					}
					cellGrid.setValue(activePoint,
							new CellState(cellGrid.getValue(activePoint).potential, PathFindingTag.Reached));
					activeListIterator.remove();
				}
			}
			activeList.addAll(newActiveList);
		}

		logger.debug("#update / #vertices: " + nUpdates + " / " + cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Reached).count());
		nUpdates = 0;
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

	@Override
	public void update() {
		timeCostFunction.update();
		solve();
	}

	@Override
	public boolean needsUpdate() {
		return timeCostFunction.needsUpdate();
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
