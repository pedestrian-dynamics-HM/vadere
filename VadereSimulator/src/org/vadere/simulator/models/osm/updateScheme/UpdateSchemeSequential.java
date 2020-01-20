package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.OSMBehaviorController;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

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
		this.osmBehaviorController = new OSMBehaviorController();
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		clearStrides(topography);
		update(topography.getElements(Pedestrian.class), currentTimeInSec, timeStepInSec);
	}

	protected void update(@NotNull final Collection<Pedestrian> pedestrianOSMS, final double currentTimeInSec, final double timeStepInSec) {
		// TODO: Clarify with Bene if we can call "clearStrides()" here directly like in "UpdateSchemeEventDriven"
		//   and omit in in invoked "update()".
		for (Pedestrian pedestrian : pedestrianOSMS) {
			if(!skipUdate.contains(pedestrian)) {
				update((PedestrianOSM) pedestrian, currentTimeInSec, timeStepInSec);
			}
			//pedestrian.update(timeStepInSec, -1, CallMethod.SEQUENTIAL);
		}
		skipUdate.clear();
	}

	protected void update(@NotNull final PedestrianOSM pedestrian, final double currentTimeInSec, final double timeStepInSec) {
		Stimulus mostImportantStimulus = pedestrian.getMostImportantStimulus();

		// for the first step after creation, timeOfNextStep has to be initialized
		if (pedestrian.getTimeOfNextStep() == Pedestrian.INVALID_NEXT_EVENT_TIME) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
		}

		SelfCategory selfCategory = pedestrian.getSelfCategory();

		if (selfCategory == SelfCategory.TARGET_ORIENTED) {
			pedestrian.clearStrides();
			stepForward(pedestrian, currentTimeInSec, timeStepInSec);
		} else if (selfCategory == SelfCategory.COOPERATIVE) {
			pedestrian.clearStrides();
			PedestrianOSM candidate = osmBehaviorController.findSwapCandidate(pedestrian, topography);

			if(candidate != null) {
				osmBehaviorController.swapPedestrians(pedestrian, candidate, topography);
				// here we update not only pedestrian but also candidate, therefore candidate is already treated and will be skipped.
				skipUdate.add(candidate);
			} else {
				stepForward(pedestrian, currentTimeInSec, timeStepInSec);
			}
		} else if (selfCategory == SelfCategory.INSIDE_THREAT_AREA) {
			osmBehaviorController.maximizeDistanceToThreatAndIncreaseSpeed(pedestrian, topography);
			osmBehaviorController.makeStepToTarget(pedestrian, topography);
		} else if (selfCategory == SelfCategory.OUTSIDE_THREAT_AREA) {
			osmBehaviorController.changeTargetToSafeZone(pedestrian, topography);
			osmBehaviorController.makeStepToTarget(pedestrian, topography);
		} else if (selfCategory == SelfCategory.WAIT) {
			osmBehaviorController.wait(pedestrian, timeStepInSec);
		} else if (selfCategory == SelfCategory.CHANGE_TARGET) {
			osmBehaviorController.changeTarget(pedestrian, topography);
			// Set time of next step. Otherwise, the internal OSM event queue hangs endlessly.
			pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
		}
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
