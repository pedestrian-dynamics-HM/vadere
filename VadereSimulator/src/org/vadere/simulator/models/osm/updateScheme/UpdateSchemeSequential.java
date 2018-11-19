package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;

public class UpdateSchemeSequential implements UpdateSchemeOSM {

	private final Topography topography;

	public UpdateSchemeSequential(@NotNull final Topography topography) {
		this.topography = topography;
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		clearStrides(topography);
		update(topography.getElements(Pedestrian.class), timeStepInSec);
	}

	protected void update(@NotNull final Collection<Pedestrian> pedestrianOSMS, final double timeStepInSec) {
		for (Pedestrian pedestrian : pedestrianOSMS) {
			update((PedestrianOSM) pedestrian, timeStepInSec);
			//pedestrian.update(timeStepInSec, -1, CallMethod.SEQUENTIAL);
		}
	}

	protected void update(@NotNull final PedestrianOSM pedestrian, final double timeStepInSec) {
		VPoint oldPosition = pedestrian.getPosition();
		pedestrian.clearStrides();
		pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());

		while (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
			pedestrian.updateNextPosition();
			makeStep(topography, pedestrian, timeStepInSec);
			pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());
		}

		topography.moveElement(pedestrian, oldPosition);
	}

	@Override
	public void elementAdded(Pedestrian element) {}

	@Override
	public void elementRemoved(Pedestrian element) {}
}
