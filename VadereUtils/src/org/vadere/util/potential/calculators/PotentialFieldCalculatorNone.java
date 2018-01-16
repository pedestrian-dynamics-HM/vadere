package org.vadere.util.potential.calculators;

import org.vadere.util.geometry.shapes.VPoint;
import java.util.function.Function;

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
    public Function<VPoint, Double> getPotentialField() {
        return p -> 0.0;
    }

    @Override
    public double getPotential(double x, double y) {
        return 0;
    }
}
