package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.OSMBehaviorController;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UpdateSchemeSequential implements UpdateSchemeOSM {

	private final Topography topography;
	private final OSMBehaviorController osmBehaviorController;
	private final Set<PedestrianOSM> skipUdate;

	public UpdateSchemeSequential(@NotNull final Topography topography) {
		this.topography = topography;
		this.skipUdate = new HashSet<>();
		this.osmBehaviorController = new OSMBehaviorController(topography);
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		clearStrides(topography);
		update(topography.getElements(Pedestrian.class), currentTimeInSec, timeStepInSec);
	}

	protected void update(@NotNull final Collection<Pedestrian> pedestrianOSMS, final double currentTimeInSec, final double timeStepInSec) {
		for (Pedestrian pedestrian : pedestrianOSMS) {
			if(!skipUdate.contains(pedestrian)) {
				update((PedestrianOSM) pedestrian, currentTimeInSec, timeStepInSec);
			}
		}
		skipUdate.clear();
	}

	protected void update(@NotNull final PedestrianOSM pedestrian, final double currentTimeInSec, final double timeStepInSec) {
		// for the first step after creation, timeOfNextStep has to be initialized
		if (pedestrian.getTimeOfNextStep() == Pedestrian.INVALID_NEXT_EVENT_TIME) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
		}

		//TODO refactor -> use osmBehaviorcontroller. makeStepToTarget instead of stepForward
		stepForward(pedestrian, currentTimeInSec, timeStepInSec);

	}

	private void stepForward(@NotNull final PedestrianOSM pedestrian, final double simTimeInSec, final double timeStepInSec) {

		while (pedestrian.getTimeOfNextStep() < simTimeInSec) {
			pedestrian.updateNextPosition();
			osmBehaviorController.makeStep(pedestrian, topography, timeStepInSec);
			pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
		}
	}



	@Override
	public void elementAdded(Pedestrian element) {}

	@Override
	public void elementRemoved(Pedestrian element) {}
}
