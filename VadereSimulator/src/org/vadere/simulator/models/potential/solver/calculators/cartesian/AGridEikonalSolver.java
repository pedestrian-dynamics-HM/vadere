package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.data.cellgrid.CellGrid;
import java.util.function.Function;

// TODO: not necessarily abstract!
public abstract class AGridEikonalSolver implements GridEikonalSolver {
	protected CellGrid potentialField;
	private final double unknownPenalty;
	private final double weight;

	public AGridEikonalSolver(final CellGrid potentialField, final double unknownPenalty, final double weight) {
		this.potentialField = potentialField;
		this.unknownPenalty = unknownPenalty;
		this.weight = weight;
	}

    public Function<IPoint, Double> getPotentialField() {
        CellGrid clone = potentialField.clone();
        return p -> getPotential(clone, p.getX(), p.getY(), unknownPenalty, weight);
    }

    @Override
    public CellGrid getCellGrid() {
	    return potentialField;
    }

    @Override
	public double getPotential(final double x, final double y) {
		return getPotential(x, y, unknownPenalty, weight);
    }

	@Override
	public double getPotential(IPoint pos, double unknownPenalty, double weight) {
		return getPotential(potentialField, pos, unknownPenalty, weight);
	}

}
