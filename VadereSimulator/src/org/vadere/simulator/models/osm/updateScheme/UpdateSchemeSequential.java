package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.combinedPotentials.CombinedPotentialStrategy;
import org.vadere.state.events.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class UpdateSchemeSequential implements UpdateSchemeOSM {

	private final Topography topography;

	public UpdateSchemeSequential(@NotNull final Topography topography) {
		this.topography = topography;
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		clearStrides(topography);
		update(topography.getElements(Pedestrian.class), timeStepInSec);
	}

	protected void update(@NotNull final Collection<Pedestrian> pedestrianOSMS, final double timeStepInSec) {
		for (Pedestrian pedestrian : pedestrianOSMS) {
			update((PedestrianOSM) pedestrian, timeStepInSec);
			//pedestrian.update(timeStepInSec, -1, CallMethod.SEQUENTIAL);
		}
	}

	protected void update(@NotNull final PedestrianOSM pedestrian, final double timeStepInSec) {
		Event mostImportantEvent = pedestrian.getMostImportantEvent();

		if (mostImportantEvent instanceof ElapsedTimeEvent) {
			VPoint oldPosition = pedestrian.getPosition();
			pedestrian.clearStrides();
			pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);

			while (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
				pedestrian.updateNextPosition();
				makeStep(topography, pedestrian, timeStepInSec);
			}

		} else if (mostImportantEvent instanceof WaitEvent || mostImportantEvent instanceof WaitInAreaEvent) {
			pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
		} else if (mostImportantEvent instanceof BangEvent) {
			// Watch out: For testing purposes, a bang event changes only
			// the "CombinedPotentialStrategy". The agent does not move here!
			// Therefore, trigger only a single bang event and then use "ElapsedTimeEvent"
			BangEvent bangEvent = (BangEvent) mostImportantEvent;
			Target bangOrigin = topography.getTarget(bangEvent.getOriginAsTargetId());

			LinkedList<Integer> nextTarget = new LinkedList<>();
			nextTarget.add(bangOrigin.getId());

			pedestrian.setTargets(nextTarget);
			pedestrian.setCombinedPotentialStrategy(CombinedPotentialStrategy.TARGET_DISTRACTION_STRATEGY);
		}
	}

	@Override
	public void elementAdded(Pedestrian element) {}

	@Override
	public void elementRemoved(Pedestrian element) {}
}
