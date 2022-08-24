package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.OSMBehaviorController;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.combinedPotentials.CombinedPotentialStrategy;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * @author Benedikt Zoennchen
 */
public class UpdateSchemeEventDriven implements UpdateSchemeOSM {

	private final Topography topography;
	protected PriorityQueue<PedestrianOSM> pedestrianEventsQueue;
	private final OSMBehaviorController osmBehaviorController;

	public UpdateSchemeEventDriven(@NotNull final Topography topography) {
		this.topography = topography;
		this.pedestrianEventsQueue = new PriorityQueue<>(100, new ComparatorPedestrianOSM());
		this.pedestrianEventsQueue.addAll(topography.getElements(PedestrianOSM.class));
		this.osmBehaviorController = new OSMBehaviorController();
	}

	@Override
	public void update(final double timeStepInSec, final double currentTimeInSec) {
		clearStrides(topography);
		if(!pedestrianEventsQueue.isEmpty()) {
			// event driven update ignores time credits!
			while (pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec) {
				PedestrianOSM ped = pedestrianEventsQueue.poll();
				update(ped, timeStepInSec, currentTimeInSec);
				//System.out.println(ped.getId());
				pedestrianEventsQueue.add(ped);
			}
		}
	}

	protected void update(@NotNull final PedestrianOSM pedestrian, final double timeStepInSec, final double currentTimeInSec) {
		// for the first step after creation, timeOfNextStep has to be initialized
		if (pedestrian.getTimeOfNextStep() == Pedestrian.INVALID_NEXT_EVENT_TIME) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
			return;
		}

		SelfCategory selfCategory = pedestrian.getSelfCategory();

		// TODO: Maybe, use a state table with function pointers to a template function myFunc(ped, topography, time)
		if (selfCategory == SelfCategory.TARGET_ORIENTED) {
			pedestrian.setCombinedPotentialStrategy(CombinedPotentialStrategy.TARGET_ATTRACTION_STRATEGY);
			osmBehaviorController.makeStepToTarget(pedestrian, topography);
		} else if (selfCategory == SelfCategory.COOPERATIVE) {
			PedestrianOSM candidate = osmBehaviorController.findSwapCandidate(pedestrian, topography);

			if (candidate != null) {
				pedestrianEventsQueue.remove(candidate);
				osmBehaviorController.swapPedestrians(pedestrian, candidate, topography);
				pedestrianEventsQueue.add(candidate);
			} else {
				osmBehaviorController.makeStepToTarget(pedestrian, topography);
			}
		} else if (selfCategory == SelfCategory.THREATENED) {
			osmBehaviorController.changeToTargetRepulsionStrategyAndIncreaseSpeed(pedestrian, topography);
			osmBehaviorController.makeStepToTarget(pedestrian, topography);
		} else if (selfCategory == SelfCategory.COMMON_FATE) {
			osmBehaviorController.changeTargetToSafeZone(pedestrian, topography);
			osmBehaviorController.makeStepToTarget(pedestrian, topography);
		} else if (selfCategory == SelfCategory.WAIT) {
			osmBehaviorController.wait(pedestrian, topography, timeStepInSec);
			// needed for postvis to correctly reproduce state.
			pedestrian.getTrajectory().add(new FootStep(pedestrian.getPosition(), pedestrian.getPosition(), currentTimeInSec, pedestrian.getTimeOfNextStep()));
		} else if (selfCategory == SelfCategory.CHANGE_TARGET) {
			osmBehaviorController.changeTarget(pedestrian, topography);
			// needed for postvis to correctly reproduce state.
			pedestrian.getTrajectory().add(new FootStep(pedestrian.getPosition(), pedestrian.getPosition(), currentTimeInSec, pedestrian.getTimeOfNextStep()));
		} else if (selfCategory == SelfCategory.SOCIAL_DISTANCING){
			osmBehaviorController.changeRepulsion(pedestrian);
			osmBehaviorController.makeStepToTarget(pedestrian, topography);
		}
	}

	@Override
	public void elementRemoved(@NotNull final Pedestrian element) {
		pedestrianEventsQueue.remove(element);
	}

	@Override
	public void elementAdded(final Pedestrian element) {
		pedestrianEventsQueue.add((PedestrianOSM) element);
	}

	/**
	 * Compares the time of the next possible move.
	 */
	private class ComparatorPedestrianOSM implements Comparator<PedestrianOSM> {
		@Override
		public int compare(PedestrianOSM ped1, PedestrianOSM ped2) {
			int timeCompare = Double.compare(ped1.getTimeOfNextStep(), ped2.getTimeOfNextStep());
			if(timeCompare != 0) {
				return timeCompare;
			}
			else {
				if(ped1.getId() < ped2.getId()) {
					return -1;
				}
				else {
					return 1;
				}
			}
		}
	}
}
