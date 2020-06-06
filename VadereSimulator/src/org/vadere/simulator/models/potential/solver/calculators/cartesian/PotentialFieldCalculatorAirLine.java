package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import org.vadere.util.data.cellgrid.CellGrid;

import java.awt.*;
import java.util.LinkedList;

public class PotentialFieldCalculatorAirLine extends AGridEikonalSolver {

	private CellGrid potentialField;
	private LinkedList<Point> targetPoints;

	PotentialFieldCalculatorAirLine(
			final CellGrid potentialField,
			final LinkedList<Point> targetPoints,
			final double knownPenalty,
            final double weight) {
		super(potentialField, knownPenalty, weight);
		this.potentialField = potentialField;
		this.targetPoints = targetPoints;
	}

	@Override
	public void solve() {

		for (int x = 0; x < potentialField.getWidth(); ++x) {
			for (int y = 0; y < potentialField.getHeight(); ++y) {
				if (potentialField.getValue(x, y).tag.accessible) {
					double minTargetDistance = Double.MAX_VALUE;

					for (Point p : targetPoints) {
						double targetDistance = potentialField.pointDistance(p, new Point(x, y));
						if (targetDistance < minTargetDistance) {
							minTargetDistance = targetDistance;
						}
					}
					potentialField.getValue(x, y).potential = minTargetDistance;
				}
			}
		}

		System.out.println("AirLine is done.");
	}

	@Override
	public void update() {}

	@Override
	public boolean needsUpdate() {
		return false;
	}
}
