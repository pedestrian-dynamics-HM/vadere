package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class UpdateSchemeShuffle extends UpdateSchemeSequential {

	private final Random random;

	public UpdateSchemeShuffle(@NotNull final Topography topography, final Random random) {
		super(topography);
		this.random = random;
	}

	@Override
	protected void update(@NotNull Collection<Pedestrian> pedestrianOSMS, double currentTimeInSec, double timeStepInSec) {
		List<Pedestrian> shuffledList = new ArrayList<>(pedestrianOSMS.size());
		shuffledList.addAll(pedestrianOSMS);
		Collections.shuffle(shuffledList, random);
		super.update(shuffledList, currentTimeInSec, timeStepInSec);
	}
}
