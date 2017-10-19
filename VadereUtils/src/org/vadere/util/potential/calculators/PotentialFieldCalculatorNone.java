package org.vadere.util.potential.calculators;

public class PotentialFieldCalculatorNone implements EikonalSolver {

	@Override
	public void initialize() {}

	@Override
	public void update() {}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public double getValue(double x, double y) {
		return 0;
	}
}
