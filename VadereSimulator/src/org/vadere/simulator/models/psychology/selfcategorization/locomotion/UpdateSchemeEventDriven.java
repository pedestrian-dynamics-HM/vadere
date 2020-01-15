package org.vadere.simulator.models.psychology.selfcategorization.locomotion;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.psychology.selfcategorization.PedestrianSelfCatThreat;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.*;
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

		Stimulus mostImportantStimulus = pedestrian.getMostImportantStimulus();

		if (mostImportantStimulus instanceof ElapsedTime) {
			if (pedestrian.getSelfCategory() == SelfCategory.TARGET_ORIENTED) {
				osmBehaviorController.makeStepToTarget(pedestrian, topography);
			} else if (pedestrian.getSelfCategory() == SelfCategory.COOPERATIVE) {
				PedestrianOSM candidate = osmBehaviorController.findSwapCandidate(pedestrian, topography);

				if(candidate != null) {
					pedestrianEventsQueue.remove(candidate);
					osmBehaviorController.swapPedestrians(pedestrian, candidate, topography);
					pedestrianEventsQueue.add((PedestrianSelfCatThreat)candidate);
				} else {
					osmBehaviorController.makeStepToTarget(pedestrian, topography);
				}
				// TODO: else if (pedestrian.getSelfCategory() == SelfCategory.LEFT_BANG_AREA)
				//  Change target to safe zone and "makeStepToTarget()".
			}
		} else if (mostImportantStimulus instanceof Wait || mostImportantStimulus instanceof WaitInArea) {
			osmBehaviorController.wait(pedestrian, timeStepInSec);
		} else if (mostImportantStimulus instanceof Bang) {
			// TODO: Increase pedestrians free-flow velocity by a fixed factor of 1.5 or 2.
			osmBehaviorController.reactToBang(pedestrian, topography);
			osmBehaviorController.makeStepToTarget(pedestrian, topography);
		} else if (mostImportantStimulus instanceof ChangeTarget) {
			osmBehaviorController.reactToTargetChange(pedestrian, topography);
			// Set time of next step. Otherwise, the internal OSM event queue hangs endlessly.
			pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
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
