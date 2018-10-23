package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.data.cellgrid.IPotentialPoint;
import org.vadere.util.data.cellgrid.PathFindingTag;

public class PotentialPoint extends EikMeshPoint implements IPotentialPoint {

	private double potential;
	private PathFindingTag tag;

	public PotentialPoint(double x, double y) {
		super(x, y, false);
		this.potential = Double.MAX_VALUE;
		this.tag = PathFindingTag.Undefined;
	}

	@Override
	public double getPotential() {
		return potential;
	}

	@Override
	public void setPotential(final double potential) {
		this.potential = potential;
	}

	@Override
	public void setPathFindingTag(@NotNull final PathFindingTag tag) {
		this.tag = tag;
	}

	@Override
	public PathFindingTag getPathFindingTag() {
		return tag;
	}
}
