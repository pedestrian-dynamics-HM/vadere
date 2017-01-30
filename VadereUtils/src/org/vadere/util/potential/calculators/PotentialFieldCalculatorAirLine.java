package org.vadere.util.potential.calculators;

import java.awt.Point;
import java.util.LinkedList;

import org.vadere.util.potential.CellGrid;

public class PotentialFieldCalculatorAirLine extends AbstractGridEikonalSolver {

	private CellGrid potentialField;
	private LinkedList<Point> targetPoints;

	PotentialFieldCalculatorAirLine(
			final CellGrid potentialField,
			final LinkedList<Point> targetPoints,
			final double knownPenalty) {
		super(potentialField, knownPenalty);
		this.potentialField = potentialField;
		this.targetPoints = targetPoints;
	}

	@Override
	public void initialize() {

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

	public boolean isValidPoint(Point point) {
		return potentialField.isValidPoint(point);
	}
}
