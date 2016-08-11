package org.vadere.simulator.models.osm.updateScheme;

import org.vadere.simulator.models.osm.PedestrianOSM;

public class UpdateSchemeSequential implements UpdateSchemeOSM {

	private final PedestrianOSM pedestrian;

	public UpdateSchemeSequential(PedestrianOSM pedestrian) {
		this.pedestrian = pedestrian;
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec, CallMethod callMethod) {
		pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());

		while (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
			pedestrian.updateNextPosition();
			pedestrian.makeStep(timeStepInSec);
			pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());
		}
	}
}
