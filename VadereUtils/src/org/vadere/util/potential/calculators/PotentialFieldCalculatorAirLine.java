package org.vadere.util.potential.calculators;

import java.awt.Point;
import java.util.LinkedList;

import org.vadere.util.potential.CellGrid;

public class PotentialFieldCalculatorAirLine implements EikonalSolver {

	private CellGrid potentialField;
	private LinkedList<Point> targetPoints;

	PotentialFieldCalculatorAirLine(CellGrid potentialField,
			LinkedList<Point> targetPoints) {
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
						double targetDistance = potentialField.pointDistance(p,
								new Point(x, y));

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

	@Override
	public CellGrid getPotentialField() {
		return potentialField;
	}

}
