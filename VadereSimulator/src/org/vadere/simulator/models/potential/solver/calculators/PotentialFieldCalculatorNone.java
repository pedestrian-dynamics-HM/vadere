package org.vadere.simulator.models.potential.solver.calculators;

import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Function;

public class PotentialFieldCalculatorNone implements EikonalSolver {

	@Override
	public void solve() {}

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

	/**
	 * Returns an empty mesh.
	 *
	 * @return an empty mesh
	 */
	@Override
	public IMesh<?, ?, ?> getDiscretization() {
		return new PMesh();
	}
}
