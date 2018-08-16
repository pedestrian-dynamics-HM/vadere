package org.vadere.simulator.models.osm.updateScheme;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

public class UpdateSchemeEventDriven implements UpdateSchemeOSM {

	private final PedestrianOSM pedestrian;
	private final Topography topography;

	public UpdateSchemeEventDriven(PedestrianOSM pedestrian, Topography topography) {
		this.pedestrian = pedestrian;
		this.topography = topography;
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec, CallMethod callMethod) {

		VPoint oldPosition = pedestrian.getPosition();

		// for the first step after creation, timeOfNextStep has to be initialized
		if (pedestrian.getTimeOfNextStep() == 0) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
		}

		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());
		pedestrian.updateNextPosition();
		pedestrian.makeStep(pedestrian.getDurationNextStep());
		pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());

		topography.moveElement(pedestrian, oldPosition);
	}

}
