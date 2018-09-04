package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Topography;

/**
 * @author Benedikt Zoennchen
 */
public class UpdateSchemeCLParellel extends UpdateSchemeParallel {

	public UpdateSchemeCLParellel(@NotNull final Topography pedestrian) {
		super(pedestrian);
	}

}
