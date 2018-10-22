package org.vadere.util.potential.calculators;

import org.vadere.geometry.shapes.IPoint;
import org.vadere.geometry.shapes.VPoint;
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
	public double getPotential(IPoint pos, double unknownPenalty, double weight) {
		return 0;
	}

	@Override
    public Function<IPoint, Double> getPotentialField() {
        return p -> 0.0;
    }

    @Override
    public double getPotential(double x, double y) {
        return 0;
    }
}
