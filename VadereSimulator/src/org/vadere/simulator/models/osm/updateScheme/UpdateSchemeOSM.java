package org.vadere.simulator.models.osm.updateScheme;


import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.opencl.CLOptimalStepsModel;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElementAddListener;
import org.vadere.state.scenario.DynamicElementRemoveListener;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.UpdateType;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.opencl.OpenCLException;
import org.vadere.util.potential.calculators.EikonalSolver;

import java.util.Random;

public interface UpdateSchemeOSM extends DynamicElementRemoveListener<Pedestrian>, DynamicElementAddListener<Pedestrian> {

	enum CallMethod {
		SEEK, MOVE, CONFLICTS, STEPS, RETRY, SEQUENTIAL, EVENT_DRIVEN
	}

	void update(double timeStepInSec, double currentTimeInSec);

	static UpdateSchemeOSM create(@NotNull final UpdateType updateType, @NotNull final Topography topography, final Random random) {
		switch (updateType) {
			case SEQUENTIAL: return new UpdateSchemeSequential(topography);
			case PARALLEL: return new UpdateSchemeParallel(topography);
			case EVENT_DRIVEN: return new UpdateSchemeEventDriven(topography);
			case SHUFFLE: return new UpdateSchemeShuffle(topography, random);
			default: throw new IllegalArgumentException(updateType + " is not supported.");
		}
	}

	static UpdateSchemeOSM createOpenCLUpdateScheme(
			@NotNull final Topography topography,
			@NotNull final AttributesOSM attributesOSM,
			@NotNull final AttributesFloorField attributesFloorField,
			@NotNull final double pedestrianPotentialWidth,
			@NotNull final EikonalSolver targetEikonalSolver,
			@NotNull final EikonalSolver distanceEikonalSolver) {

		try {
			CLOptimalStepsModel clOptimalStepsModel = new CLOptimalStepsModel(
					attributesOSM,
					attributesFloorField,
					new VRectangle(topography.getBounds()),
					pedestrianPotentialWidth, // max step length + function width
					targetEikonalSolver,
					distanceEikonalSolver);

			return new UpdateSchemeCLParellel(topography, clOptimalStepsModel);

		} catch (OpenCLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

}
