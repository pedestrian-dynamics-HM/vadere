package org.vadere.util.potential.calculators.cartesian;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.potential.CellGrid;
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

    public Function<VPoint, Double> getPotentialField() {
        CellGrid clone = potentialField.clone();
        return p -> getPotential(clone, p.getX(), p.getY());
    }

    public CellGrid getCellGrid() {
	    return potentialField;
    }


    private double getPotential(final CellGrid cellGrid, final double x, final double y) {
		return getPotential(cellGrid, x, y, unknownPenalty, weight);
    }

    @Override
	public double getPotential(final double x, final double y) {
        return getPotential(potentialField, x, y);
	}
}
