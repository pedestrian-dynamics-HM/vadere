package org.vadere.util.potential.calculators;


import org.vadere.util.potential.CellGrid;

import java.awt.*;

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
    public CellGrid getPotentialField() {
        throw new UnsupportedOperationException("not jet implemented.");
    }

    @Override
	public double getValue(double x, double y) {
		return 0;
	}

	public boolean isValidPoint(Point point) {
		return false;
	}
}
