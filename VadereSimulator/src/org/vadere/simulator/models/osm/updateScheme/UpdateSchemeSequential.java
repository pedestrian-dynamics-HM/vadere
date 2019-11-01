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

/**
 * TODO: explain the concept of timeCredit!
 * TODO: in the long term, replace timeCredit by eventTime (see event driven update)!
 */
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

		if (mostImportantStimulus instanceof ElapsedTime) {
			pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
			pedestrian.clearStrides();
			if (pedestrian.getSelfCategory() == SelfCategory.TARGET_ORIENTED) {
				useTimeCredit(pedestrian, timeStepInSec);
			} else if (pedestrian.getSelfCategory() == SelfCategory.COOPERATIVE) {
				PedestrianOSM candidate = osmBehaviorController.findSwapCandidate(pedestrian, topography);
				if(candidate != null) {
					candidate.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
					osmBehaviorController.swapPedestrians(pedestrian, candidate, topography);
					// here we update not only pedestrian but also candidate, therefore candidate is already treated and will be skipped.
					skipUdate.add(candidate);
				} else {
					useTimeCredit(pedestrian, timeStepInSec);
				}
			}
		} else if (mostImportantStimulus instanceof Wait || mostImportantStimulus instanceof WaitInArea) {
			osmBehaviorController.wait(pedestrian, timeStepInSec);
		} else if (mostImportantStimulus instanceof Bang) {
			osmBehaviorController.reactToBang(pedestrian, topography);
		} else if (mostImportantStimulus instanceof ChangeTarget) {
			osmBehaviorController.reactToTargetChange(pedestrian, topography);
		}
	}

	private void useTimeCredit(@NotNull final PedestrianOSM pedestrian, final double timeStepInSec) {
		while (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
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
