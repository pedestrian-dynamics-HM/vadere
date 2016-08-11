package org.vadere.util.potential.calculators;


import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;

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
		return new CellGrid(0, 0, 1, new CellState());
	}

}
