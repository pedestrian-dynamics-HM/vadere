package org.vadere.simulator.models.psychology.selfcategorization.locomotion;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.OSMBehaviorController;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.combinedPotentials.CombinedPotentialStrategy;
import org.vadere.simulator.models.psychology.selfcategorization.PedestrianSelfCatThreat;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.*;

import java.util.Comparator;
import java.util.PriorityQueue;

public class UpdateSchemeEventDriven implements DynamicElementAddListener, DynamicElementRemoveListener {

	private final Topography topography;
	protected PriorityQueue<PedestrianSelfCatThreat> pedestrianEventsQueue;
	private final OSMBehaviorController osmBehaviorController;

	public UpdateSchemeEventDriven(@NotNull final Topography topography) {
		this.topography = topography;
		this.pedestrianEventsQueue = new PriorityQueue<>(100, new ComparatorPedestrianSelfCatThreat());
		this.pedestrianEventsQueue.addAll(topography.getElements(PedestrianSelfCatThreat.class));
		this.osmBehaviorController = new OSMBehaviorController();
	}

	public void update(final double timeStepInSec, final double currentTimeInSec) {
		clearStrides(topography);
		if(!pedestrianEventsQueue.isEmpty()) {
			// event driven update ignores time credits!
			while (pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec) {
				PedestrianSelfCatThreat ped = pedestrianEventsQueue.poll();
				update(ped, timeStepInSec, currentTimeInSec);
				pedestrianEventsQueue.add(ped);
			}
		}
	}

	private void clearStrides(@NotNull final Topography topography) {
		/*
		 * strides and foot steps have no influence on the simulation itself, i.e. they are saved to analyse trajectories
		 */
		for(PedestrianSelfCatThreat pedestrian : topography.getElements(PedestrianSelfCatThreat.class)) {
			pedestrian.clearStrides();
			pedestrian.clearFootSteps();
		}
	}

	private void update(@NotNull final PedestrianSelfCatThreat pedestrian, final double timeStepInSec, final double currentTimeInSec) {
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
				pedestrianEventsQueue.add((PedestrianSelfCatThreat) candidate);
			} else {
				osmBehaviorController.makeStepToTarget(pedestrian, topography);
			}
		} else if (selfCategory == SelfCategory.THREATENED) {
			osmBehaviorController.changeToTargetRepulsionStrategyAndIncreaseSpeed(pedestrian, topography);
			osmBehaviorController.makeStepToTarget(pedestrian, topography);
		} else if (selfCategory == SelfCategory.COMMON_FATE) {
			// TODO: Check if "changeToTargetRepulsionStrategyAndIncreaseSpeed()" is really necessary here.
			//   It is necessary here if pedestrian was "THREATENED" before but locomotion layer
			//   had no time to call this method.
			osmBehaviorController.changeToTargetRepulsionStrategyAndIncreaseSpeed(pedestrian, topography);
			osmBehaviorController.changeTargetToSafeZone(pedestrian, topography);
			osmBehaviorController.makeStepToTarget(pedestrian, topography);
		} else if (selfCategory == SelfCategory.WAIT) {
			osmBehaviorController.wait(pedestrian, topography, timeStepInSec);
		} else if (selfCategory == SelfCategory.CHANGE_TARGET) {
			osmBehaviorController.changeTarget(pedestrian, topography);
		} else if (selfCategory == SelfCategory.SOCIAL_DISTANCING){
			osmBehaviorController.changeRepulsion(pedestrian);
		}
	}

	@Override
	public void elementAdded(DynamicElement element) {
		pedestrianEventsQueue.add((PedestrianSelfCatThreat) element);
	}

	@Override
	public void elementRemoved(DynamicElement element) {
		pedestrianEventsQueue.remove(element);
	}

	/**
	 * Compares the time of the next possible move.
	 */
	private class ComparatorPedestrianSelfCatThreat implements Comparator<PedestrianSelfCatThreat> {
		@Override
		public int compare(PedestrianSelfCatThreat ped1, PedestrianSelfCatThreat ped2) {
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
