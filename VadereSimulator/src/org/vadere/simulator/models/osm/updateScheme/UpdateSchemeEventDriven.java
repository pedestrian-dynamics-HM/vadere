package org.vadere.simulator.models.osm.updateScheme;

import org.vadere.simulator.models.osm.PedestrianOSM;

public class UpdateSchemeEventDriven implements UpdateSchemeOSM {

	private final PedestrianOSM pedestrian;

	public UpdateSchemeEventDriven(PedestrianOSM pedestrian) {
		this.pedestrian = pedestrian;
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec, CallMethod callMethod) {

		// for the first step after creation, timeOfNextStep has to be initialized
		if (pedestrian.getTimeOfNextStep() == 0) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
		}

		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());
		pedestrian.updateNextPosition();
		pedestrian.makeStep(pedestrian.getDurationNextStep());
		pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
	}

}
