package org.vadere.util.potential.calculators.cartesian;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.timecost.ITimeCostFunction;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
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
public class EikonalSolverFIM extends AbstractGridEikonalSolver {

	private List<VShape> targetShapes;
	private List<Point> targetPoints;
	private static Logger logger = LogManager.getLogger(EikonalSolverFIM.class);
	private ITimeCostFunction timeCostFunction;
	private boolean isHighAccuracy;
	private boolean isActiveList[][];
    private final CellGrid cellGrid;
	private final double epsilon;


	private LinkedList<Point> activeList;

	public EikonalSolverFIM(
			final CellGrid cellGrid,
			final List<VShape> targetShapes,
			final boolean isHighAccuracy,
			final ITimeCostFunction timeCostFunction,
            final double unknownPenalty,
            final double weight) {
	    super(cellGrid, unknownPenalty, weight);
	    this.cellGrid = cellGrid;
		this.timeCostFunction = timeCostFunction;
		this.isHighAccuracy = isHighAccuracy;
		this.targetShapes = targetShapes;
		this.targetPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Target)
				.collect(Collectors.toList());
		this.activeList = new LinkedList<>();
		this.epsilon = cellGrid.getResolution() / 100;

		if (targetPoints.size() == 0) {
			logger.error("PotentialFieldInitializerFastMarching::Run(): "
					+ "Warning, no target points given. Target missing or grid resolution too low.");
			return;
		}
	}

	private void init() {
		// set distances of the target neighbor points
		targetPoints.stream()
				.flatMap(p -> cellGrid.getLegitNeumannNeighborhood(p).stream())
				.filter(neighbor -> cellGrid.getValue(neighbor).tag != PathFindingTag.Obstacle)
				.forEach(neighbor -> {
					if (cellGrid.getValue(neighbor).tag != PathFindingTag.NARROW) {
						activeList.add(neighbor);
					}
					cellGrid.setValue(neighbor, new CellState(targetShapes.stream()
							.map(shape -> Math.max(0, shape.distance(cellGrid.pointToCoord(neighbor))))
							.reduce(Double.MAX_VALUE, Math::min), PathFindingTag.NARROW));

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
	}

    @Override
	public void initialize() {
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
	public void update() {}

	@Override
	public boolean needsUpdate() {
		return false;
	}

    @Override
	public ITimeCostFunction getTimeCostFunction() {
		return timeCostFunction;
	}

	@Override
	public boolean isHighAccuracy() {
		return true;
	}
}
