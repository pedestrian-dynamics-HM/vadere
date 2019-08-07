package org.vadere.simulator.models.osm.updateScheme;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.OSMBehaviorController;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.io.CollectionUtils;
import org.vadere.util.logging.Logger;

public class UpdateSchemeParallel implements UpdateSchemeOSM {

	private static Logger logger = Logger.getLogger(UpdateSchemeParallel.class);
	protected final ExecutorService executorService;
	protected final Topography topography;
	protected final Set<Pedestrian> movedPedestrians;
	private final OSMBehaviorController osmBehaviorController;

	static {
		logger.setDebug();
	}

	public UpdateSchemeParallel(@NotNull final Topography topography) {
		this.topography = topography;
		this.executorService = Executors.newFixedThreadPool(8);
		this.movedPedestrians = new HashSet<>();
		this.osmBehaviorController = new OSMBehaviorController();
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		clearStrides(topography);

		movedPedestrians.clear();
		CallMethod[] callMethods = {CallMethod.SEEK, CallMethod.MOVE, CallMethod.CONFLICTS, CallMethod.STEPS};
		List<Future<?>> futures;

		for (CallMethod callMethod : callMethods) {
			long ms = 0;
			if(callMethod == CallMethod.SEEK) {
				ms = System.currentTimeMillis();
			}


			futures = new LinkedList<>();
			for (final PedestrianOSM pedestrian : CollectionUtils.select(topography.getElements(Pedestrian.class), PedestrianOSM.class)) {
				Runnable worker = () -> update(pedestrian, timeStepInSec, currentTimeInSec, callMethod);
				futures.add(executorService.submit(worker));
			}
			collectFutures(futures);

			if(callMethod == CallMethod.SEEK) {
				ms = System.currentTimeMillis() - ms;
				logger.debug("runtime for next step computation = " + ms + " [ms]");
			}

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
				updateParallelMove(pedestrian);
				break;
			case CONFLICTS:
				updateParallelConflicts(pedestrian);
				break;
			case STEPS:
				updateParallelSteps(pedestrian, timeStepInSec, currentTimeInSec);
				break;
			default:
				throw new UnsupportedOperationException();
		}
	}

	/**
	 * Computes the next pedestrian position without update the position.
	 *
	 * @param pedestrian    the pedestrian
	 * @param timeStepInSec the duration of the time step in seconds
	 */
	protected void updateParallelSeek(@NotNull final PedestrianOSM pedestrian, double timeStepInSec) {
		pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
		if (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
			pedestrian.updateNextPosition();
			movedPedestrians.add(pedestrian);
		}
	}

	/**
	 * Sets the last and (current) position of the pedestrian. The velocity and the
	 * timeCredit will be updated later since the move operation might reverted by
	 * {@link UpdateSchemeParallel#updateParallelConflicts(PedestrianOSM)}.
	 *
	 * @param pedestrian the pedestrian
	 */
	private void  updateParallelMove(@NotNull final PedestrianOSM pedestrian) {
		if (movedPedestrians.contains(pedestrian)) {
			synchronized (topography) {
				movePedestrian(topography, pedestrian, pedestrian.getPosition(), pedestrian.getNextPosition());
			}
		}
	}

	/**
	 * Resolves conflicts: If there is any overlapping pedestrian with a smaller timeCredit,
	 * the pedestrians position will be set to his last position i.e. a rollback of the move step.
	 *
	 * @param pedestrian the pedestrian for which a rollback might be performed.
	 */
	protected void updateParallelConflicts(@NotNull final PedestrianOSM pedestrian) {
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
				synchronized (topography) {
					movePedestrian(topography, pedestrian, pedestrian.getPosition(), pedestrian.getLastPosition());
				}
			}
		}
	}

	/**
	 * Updates the timeCredit and the velocity of the pedestrian.
	 *
	 * @param pedestrian    the pedestrian
	 * @param timeStepInSec the duration of the time step in seconds
	 */
	private void updateParallelSteps(@NotNull final PedestrianOSM pedestrian, double timeStepInSec, double simTimeInSec) {
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
								.multiply(1.0 / pedestrian.getDurationNextStep()));
			}
			// wanted to make a step, but could not
			else {
				pedestrian.setVelocity(new Vector2D(0, 0));
			}

			pedestrian.getFootSteps().add(new FootStep(pedestrian.getLastPosition(), pedestrian.getNextPosition(), simTimeInSec-pedestrian.getDurationNextStep(), simTimeInSec));
		}
	}


	/**
	 * Computes a {@link List<Agent>} of pedestrians overlapping / colliding with the pedestrian
	 * @param pedestrian the pedestrian
	 * @return a {@link List<Agent>} of pedestrians colliding with the pedestrian
	 */
	protected List<Agent> getCollisionPedestrians(@NotNull final PedestrianOSM pedestrian) {
		LinkedList<Agent> result = new LinkedList<>();
		Collection<? extends Agent> agents = pedestrian.getRelevantPedestrians();

		for (Agent ped : agents) {
			if (!ped.equals(pedestrian)) {
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
