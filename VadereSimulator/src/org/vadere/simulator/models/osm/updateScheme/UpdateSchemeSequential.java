package org.vadere.simulator.models.osm.updateScheme;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

public class UpdateSchemeSequential implements UpdateSchemeOSM {

	private final PedestrianOSM pedestrian;
	private final Topography topography;

	public UpdateSchemeSequential(PedestrianOSM pedestrian, Topography topography) {
		this.pedestrian = pedestrian;
		this.topography = topography;
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec, CallMethod callMethod) {
		VPoint oldPosition = pedestrian.getPosition();

		pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());

		while (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
			pedestrian.updateNextPosition();
			pedestrian.makeStep(timeStepInSec);
			pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());
		}

		topography.moveElement(pedestrian, oldPosition);
	}
}
