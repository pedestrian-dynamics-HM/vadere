package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Comparator;
import java.util.PriorityQueue;

public class UpdateSchemeEventDriven implements UpdateSchemeOSM {

	private final Topography topography;
	private PriorityQueue<PedestrianOSM> pedestrianEventsQueue;

	public UpdateSchemeEventDriven(@NotNull final Topography topography) {
		this.topography = topography;
		this.pedestrianEventsQueue = new PriorityQueue<>(100, new ComparatorPedestrianOSM());
		this.pedestrianEventsQueue.addAll(topography.getElements(PedestrianOSM.class));
	}

	@Override
	public void update(final double timeStepInSec, final double currentTimeInSec) {
		for(PedestrianOSM pedestrianOSM : topography.getElements(PedestrianOSM.class)) {
			pedestrianOSM.clearStrides();
		}

		if(!pedestrianEventsQueue.isEmpty()) {
			// default is fixed order sequential update
			while (pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec) {
				PedestrianOSM ped = pedestrianEventsQueue.poll();
				update(ped, currentTimeInSec);
				pedestrianEventsQueue.add(ped);
			}
		}
	}

	private void update(@NotNull final PedestrianOSM pedestrian, final double currentTimeInSec) {
		// for the first step after creation, timeOfNextStep has to be initialized
		if (pedestrian.getTimeOfNextStep() == 0) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
		}

		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());
		pedestrian.updateNextPosition();
		pedestrian.makeStep(pedestrian.getDurationNextStep());
		pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
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
			// TODO [priority=low] [task=refactoring] use Double.compare() oder compareTo()
			if (ped1.getTimeOfNextStep() < ped2.getTimeOfNextStep()) {
				return -1;
			} else {
				return 1;
			}
		}
	}
}
