package org.vadere.simulator.models.osm.updateScheme;


import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.DynamicElementAddListener;
import org.vadere.state.scenario.DynamicElementRemoveListener;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.UpdateType;

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
}
