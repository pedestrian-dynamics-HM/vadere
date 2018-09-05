package org.vadere.simulator.models.osm.updateScheme;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.io.ListUtils;

public class UpdateSchemeParallel implements UpdateSchemeOSM {

	protected final ExecutorService executorService;
	protected final Topography topography;
	protected final Set<Pedestrian> movedPedestrians;

	public UpdateSchemeParallel(@NotNull final Topography topography) {
		this.topography = topography;
		this.executorService = Executors.newFixedThreadPool(8);
		this.movedPedestrians = new HashSet<>();
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		movedPedestrians.clear();
		CallMethod[] callMethods = {CallMethod.SEEK, CallMethod.MOVE, CallMethod.CONFLICTS, CallMethod.STEPS};
		List<Future<?>> futures;

		for (CallMethod callMethod : callMethods) {
			futures = new LinkedList<>();
			for (final PedestrianOSM pedestrian : ListUtils.select(topography.getElements(Pedestrian.class), PedestrianOSM.class)) {
				Runnable worker = () -> update(pedestrian, timeStepInSec, currentTimeInSec, callMethod);
				futures.add(executorService.submit(worker));
			}
			collectFutures(futures);
		}
	}

	protected void collectFutures(final List<Future<?>> futures) {
		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		// restore interruption in order to stop simulation
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	protected void update(@NotNull final PedestrianOSM pedestrian, final double timeStepInSec, double currentTimeInSec, CallMethod callMethod) {
		pedestrian.clearStrides();
		switch (callMethod) {
			case SEEK:
				updateParallelSeek(pedestrian, timeStepInSec);
				break;
			case RETRY:
				updateParallelSeek(pedestrian,0.0);
			case MOVE:
				updateParallelMove(pedestrian, timeStepInSec);
				break;
			case CONFLICTS:
				updateParallelConflicts(pedestrian, timeStepInSec);
				break;
			case STEPS:
				updateParallelSteps(pedestrian, timeStepInSec);
				break;
			default:
				throw new UnsupportedOperationException();
		}
	}

	protected void updateParallelSeek(@NotNull final PedestrianOSM pedestrian, double timeStepInSec) {
		pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());

		if (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
			pedestrian.updateNextPosition();
			movedPedestrians.add(pedestrian);
		}
	}

	private void updateParallelMove(@NotNull final PedestrianOSM pedestrian, double timeStepInSec) {
		if (movedPedestrians.contains(pedestrian)) {
			pedestrian.setLastPosition(pedestrian.getPosition());
			pedestrian.setPosition(pedestrian.getNextPosition());
		}
	}

	private void updateParallelConflicts(@NotNull final PedestrianOSM pedestrian, double timeStepInSec) {
		if (movedPedestrians.contains(pedestrian)) {
			List<Agent> others = getCollisionPedestrians(pedestrian);

			boolean undoStep = false;

			for (Agent ped : others) {
				double creditOther = ((PedestrianOSM) ped).getTimeCredit();

				if (creditOther < pedestrian.getTimeCredit()) {
					undoStep = true;
					break;
				} else if (creditOther == pedestrian.getTimeCredit()
						&& ped.getId() < pedestrian.getId()) {
					undoStep = true;
					break;
				}
			}

			if (undoStep) {
				pedestrian.setPosition(pedestrian.getLastPosition());
			}
		}
	}

	private void updateParallelSteps(@NotNull final PedestrianOSM pedestrian, double timeStepInSec) {
		if (movedPedestrians.contains(pedestrian)) {
			// did not want to make a step
			if (pedestrian.getNextPosition().equals(pedestrian.getLastPosition())) {
				pedestrian.setTimeCredit(0);
				pedestrian.setVelocity(new Vector2D(0, 0));
			}
			// made a step
			else if (!pedestrian.getPosition().equals(pedestrian.getLastPosition())) {
				pedestrian.setTimeCredit(pedestrian.getTimeCredit() - pedestrian.getDurationNextStep());

				// compute velocity by forward difference
				pedestrian.setVelocity(new Vector2D(
						pedestrian.getNextPosition().x - pedestrian.getLastPosition().x,
						pedestrian.getNextPosition().y - pedestrian.getLastPosition().y)
								.multiply(1.0 / timeStepInSec));
			}
			// wanted to make a step, but could not
			else {
				pedestrian.setVelocity(new Vector2D(0, 0));
			}
		}
	}

	private List<Agent> getCollisionPedestrians(@NotNull final PedestrianOSM pedestrian) {
		LinkedList<Agent> result = new LinkedList<>();

		for (Agent ped : pedestrian.getRelevantPedestrians()) {
			if (ped.getId() != pedestrian.getId()) {
				double thisDistance = ped.getPosition().distance(pedestrian.getPosition());

				if (ped.getRadius() + pedestrian.getRadius() > thisDistance) {
					result.add(ped);
				}
			}
		}

		return result;
	}


	@Override
	public void elementAdded(Pedestrian element) {}

	@Override
	public void elementRemoved(Pedestrian element) {}
}
