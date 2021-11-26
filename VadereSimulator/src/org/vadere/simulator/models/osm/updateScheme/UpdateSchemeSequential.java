package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.OSMBehaviorController;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.combinedPotentials.CombinedPotentialStrategy;
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
		}
		skipUdate.clear();
	}

	protected void update(@NotNull final PedestrianOSM pedestrian, final double currentTimeInSec, final double timeStepInSec) {
		// for the first step after creation, timeOfNextStep has to be initialized
		if (pedestrian.getTimeOfNextStep() == Pedestrian.INVALID_NEXT_EVENT_TIME) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
		}

		SelfCategory selfCategory = pedestrian.getSelfCategory();

		if (selfCategory == SelfCategory.TARGET_ORIENTED) {
			pedestrian.setCombinedPotentialStrategy(CombinedPotentialStrategy.TARGET_ATTRACTION_STRATEGY);
			stepForward(pedestrian, currentTimeInSec, timeStepInSec);
		} else if (selfCategory == SelfCategory.COOPERATIVE) {
			PedestrianOSM candidate = osmBehaviorController.findSwapCandidate(pedestrian, topography);

			if(candidate != null) {
				osmBehaviorController.swapPedestrians(pedestrian, candidate, topography);
				// We update "this" pedestrian and "candidate" here. Therefore, candidate is already treated and will be skipped.
				skipUdate.add(candidate);
			} else {
				stepForward(pedestrian, currentTimeInSec, timeStepInSec);
			}
		} else if (selfCategory == SelfCategory.THREATENED) {
			osmBehaviorController.changeToTargetRepulsionStrategyAndIncreaseSpeed(pedestrian, topography);
			stepForward(pedestrian, currentTimeInSec, timeStepInSec);
		} else if (selfCategory == SelfCategory.COMMON_FATE) {
			osmBehaviorController.changeTargetToSafeZone(pedestrian, topography);
			stepForward(pedestrian, currentTimeInSec, timeStepInSec);
		} else if (selfCategory == SelfCategory.WAIT) {
			osmBehaviorController.wait(pedestrian, topography, timeStepInSec);
			// needed for postvis to correctly reproduce state.
			pedestrian.getTrajectory().add(new FootStep(pedestrian.getPosition(), pedestrian.getPosition(), currentTimeInSec, pedestrian.getTimeOfNextStep()));
		} else if (selfCategory == SelfCategory.CHANGE_TARGET) {
			osmBehaviorController.changeTarget(pedestrian, topography);
			// needed for postvis to correctly reproduce state.
			pedestrian.getTrajectory().add(new FootStep(pedestrian.getPosition(), pedestrian.getPosition(), currentTimeInSec, pedestrian.getTimeOfNextStep()));
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
