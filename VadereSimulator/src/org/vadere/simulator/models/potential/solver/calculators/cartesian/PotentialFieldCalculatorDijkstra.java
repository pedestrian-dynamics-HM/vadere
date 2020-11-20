package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.math.MathUtil;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

public class PotentialFieldCalculatorDijkstra extends AGridEikonalSolver {

	private CellGrid potentialField;
	private LinkedList<Point> targetPoints;

	PotentialFieldCalculatorDijkstra(
			final CellGrid potentialField,
			final LinkedList<Point> targetPoints,
            final double unknownPenalty,
            final double weight) {
		super(potentialField, unknownPenalty, weight);
		this.potentialField = potentialField;
		this.targetPoints = targetPoints;
	}

	@Override
	public void solve() {

		ComparatorPotentialFieldValue comparator = new ComparatorPotentialFieldValue(
				potentialField);
		PriorityQueue<Point> priorityQueue = new PriorityQueue<Point>(50,
				comparator);

		priorityQueue.addAll(targetPoints);

		Point currentPoint;
		List<Point> neighbors;
		double value;

		while (!priorityQueue.isEmpty()) {
			currentPoint = priorityQueue.remove();

			if (potentialField.getValue(currentPoint).tag != PathFindingTag.Target) {
				potentialField.getValue(currentPoint).tag = PathFindingTag.Reachable;
			}

			neighbors = MathUtil.getMooreNeighborhood(currentPoint);

			for (Point neighbor : neighbors) {
				PathFindingTag neighborTag = potentialField.getValue(neighbor).tag;

				if (neighborTag == PathFindingTag.Reachable) {
					value = potentialField.getValue(currentPoint).potential
							+ potentialField.pointDistance(currentPoint,
									neighbor);

					if (value < potentialField.getValue(neighbor).potential) {
						priorityQueue.remove(neighbor);
						potentialField.getValue(neighbor).potential = value;
						priorityQueue.add(neighbor);
					}
				} else if (neighborTag == PathFindingTag.Undefined) {
					value = potentialField.getValue(currentPoint).potential
							+ potentialField.pointDistance(currentPoint,
									neighbor);
					priorityQueue.add(neighbor);

					potentialField.setValue(neighbor, new CellState(value,
							PathFindingTag.Reachable));
				}
			}
		}

		System.out.println("Dijkstra is done.");
	}

	@Override
	public void update() {}

	@Override
	public boolean needsUpdate() {
		return false;
	}
}
