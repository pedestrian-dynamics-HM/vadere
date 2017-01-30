package org.vadere.util.potential.calculators;

import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.timecost.ITimeCostFunction;

import java.util.List;


public class EikonalSolverFMMTriangulation implements EikonalSolver  {

	public EikonalSolverFMMTriangulation(final List<VShape> targetShapes,
	                                     final ITimeCostFunction timeCostFunction) {

	}

	@Override
	public void initialize() {

	}

	@Override
	public CellGrid getPotentialField() {
		return null;
	}

}
