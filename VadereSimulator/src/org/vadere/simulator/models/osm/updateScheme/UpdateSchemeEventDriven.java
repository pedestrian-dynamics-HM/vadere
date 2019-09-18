package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.OSMBehaviorController;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.combinedPotentials.CombinedPotentialStrategy;
import org.vadere.simulator.models.potential.combinedPotentials.TargetDistractionStrategy;
import org.vadere.state.behavior.SalientBehavior;
import org.vadere.state.behavior.SalientBehavior;
import org.vadere.state.events.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.MathUtil;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;

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

		Event mostImportantEvent = pedestrian.getMostImportantEvent();

		if (mostImportantEvent instanceof ElapsedTimeEvent) {
			double stepDuration = pedestrian.getDurationNextStep();
			if (pedestrian.getSalientBehavior() == SalientBehavior.TARGET_ORIENTED) {
				// this can cause problems if the pedestrian desired speed is 0 (see speed adjuster)
				pedestrian.updateNextPosition();
				osmBehaviorController.makeStep(pedestrian, topography, stepDuration);
				pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + stepDuration);
			} else if (pedestrian.getSalientBehavior() == SalientBehavior.COOPERATIVE) {
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
		} else if (mostImportantEvent instanceof WaitEvent || mostImportantEvent instanceof WaitInAreaEvent) {
			osmBehaviorController.wait(pedestrian, timeStepInSec);
		} else if (mostImportantEvent instanceof BangEvent) {
			osmBehaviorController.reactToBang(pedestrian, topography);

			// Set time of next step. Otherwise, the internal OSM event queue hangs endlessly.
			pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
		} else if (mostImportantEvent instanceof ChangeTargetEvent) {
			osmBehaviorController.reactToTargetChange(pedestrian, topography);

			// Set time of next step. Otherwise, the internal OSM event queue hangs endlessly.
			pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
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
