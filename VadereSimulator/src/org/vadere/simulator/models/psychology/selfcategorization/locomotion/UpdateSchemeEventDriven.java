package org.vadere.simulator.models.psychology.selfcategorization.locomotion;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.OSMBehaviorController;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeOSM;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.*;

import java.util.Comparator;
import java.util.PriorityQueue;

public class UpdateSchemeEventDriven implements DynamicElementAddListener, DynamicElementRemoveListener {

	private final Topography topography;
	protected PriorityQueue<PedestrianOSM> pedestrianEventsQueue;
	private final OSMBehaviorController osmBehaviorController;

	public UpdateSchemeEventDriven(@NotNull final Topography topography) {
		this.topography = topography;
		this.pedestrianEventsQueue = new PriorityQueue<>(100, new ComparatorPedestrianOSM());
		this.pedestrianEventsQueue.addAll(topography.getElements(PedestrianOSM.class));
		this.osmBehaviorController = new OSMBehaviorController();
	}

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

	private void clearStrides(@NotNull final Topography topography) {
		/**
		 * strides and foot steps have no influence on the simulation itself, i.e. they are saved to analyse trajectories
		 */
		for(PedestrianOSM pedestrianOSM : topography.getElements(PedestrianOSM.class)) {
			pedestrianOSM.clearStrides();
			pedestrianOSM.clearFootSteps();
		}
	}

	private void update(@NotNull final PedestrianOSM pedestrian, final double timeStepInSec, final double currentTimeInSec) {
		// for the first step after creation, timeOfNextStep has to be initialized
		if (pedestrian.getTimeOfNextStep() == Pedestrian.INVALID_NEXT_EVENT_TIME) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
			return;
		}

		Stimulus mostImportantStimulus = pedestrian.getMostImportantStimulus();

		if (mostImportantStimulus instanceof ElapsedTime) {
			double stepDuration = pedestrian.getDurationNextStep();
			if (pedestrian.getSelfCategory() == SelfCategory.TARGET_ORIENTED) {
				// this can cause problems if the pedestrian desired speed is 0 (see speed adjuster)
				pedestrian.updateNextPosition();
				osmBehaviorController.makeStep(pedestrian, topography, stepDuration);
				pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + stepDuration);
			} else if (pedestrian.getSelfCategory() == SelfCategory.COOPERATIVE) {
				// this call will also invoke setTimeOfNextStep
				PedestrianOSM candidate = osmBehaviorController.findSwapCandidate(pedestrian, topography);
				//TODO: Benedikt Kleinmeier:
				if(candidate != null) {
					//if(Math.abs(pedestrian.getTimeOfNextStep() - candidate.getTimeOfNextStep()) < MathUtil.EPSILON) {
						pedestrianEventsQueue.remove(candidate);
						osmBehaviorController.swapPedestrians(pedestrian, candidate, topography);
						pedestrianEventsQueue.add(candidate);
					/*} else {
						pedestrian.setTimeOfNextStep(candidate.getTimeOfNextStep());
					}*/
				} else {
					pedestrian.updateNextPosition();
					osmBehaviorController.makeStep(pedestrian, topography, pedestrian.getDurationNextStep());
					pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
				}
			}
		} else if (mostImportantStimulus instanceof Wait || mostImportantStimulus instanceof WaitInArea) {
			osmBehaviorController.wait(pedestrian, timeStepInSec);
		} else if (mostImportantStimulus instanceof Bang) {
			osmBehaviorController.reactToBang(pedestrian, topography);

			// Set time of next step. Otherwise, the internal OSM event queue hangs endlessly.
			pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
		} else if (mostImportantStimulus instanceof ChangeTarget) {
			osmBehaviorController.reactToTargetChange(pedestrian, topography);

			// Set time of next step. Otherwise, the internal OSM event queue hangs endlessly.
			pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
		}
	}

	@Override
	public void elementAdded(DynamicElement element) {
		pedestrianEventsQueue.add((PedestrianOSM) element);
	}

	@Override
	public void elementRemoved(DynamicElement element) {
		pedestrianEventsQueue.remove(element);
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
