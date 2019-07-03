package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.OSMBehaviorController;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.combinedPotentials.CombinedPotentialStrategy;
import org.vadere.state.behavior.SalientBehavior;
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
	private final OSMBehaviorController osmBehaviorController;

	public UpdateSchemeSequential(@NotNull final Topography topography) {
		this.topography = topography;
		this.osmBehaviorController = new OSMBehaviorController();
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

			if (pedestrian.getSalientBehavior() == SalientBehavior.TARGET_ORIENTED) {
				while (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
					pedestrian.updateNextPosition();
					osmBehaviorController.makeStep(pedestrian, topography, timeStepInSec);
					pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());

				}
			} else if (pedestrian.getSalientBehavior() == SalientBehavior.COOPERATIVE) {
				osmBehaviorController.swapWithClosestCooperativePedestrian(pedestrian, topography);
			}

		} else if (mostImportantEvent instanceof WaitEvent || mostImportantEvent instanceof WaitInAreaEvent) {
			osmBehaviorController.wait(pedestrian);
		} else if (mostImportantEvent instanceof BangEvent) {
			osmBehaviorController.reactToBang(pedestrian, topography);
		}
	}

	@Override
	public void elementAdded(Pedestrian element) {}

	@Override
	public void elementRemoved(Pedestrian element) {}
}
