package org.vadere.simulator.models.osm.opencl;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.opencl.OpenCLException;


public abstract class CLAbstractOSMParallel extends CLAbstractOSM {

	public CLAbstractOSMParallel(
			@NotNull final AttributesOSM attributesOSM,
			@NotNull final AttributesFloorField attributesFloorField,
			@NotNull final VRectangle bound,
			@NotNull final EikonalSolver targetPotential,
			@NotNull final EikonalSolver obstaclePotential,
			final int device,
			final double cellSize) throws OpenCLException {
		super(attributesOSM, attributesFloorField, bound, targetPotential, obstaclePotential, device, cellSize);
	}
}
