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
import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.io.CollectionUtils;
import org.vadere.util.logging.Logger;

/**
 * TODO: should also use OSMBehaviorController.makeStep !
 */
public class UpdateSchemeParallel implements UpdateSchemeOSM {

	private static Logger logger = Logger.getLogger(UpdateSchemeParallel.class);
	protected final ExecutorService executorService;
	protected final Topography topography;
	protected final Set<Pedestrian> movedPedestrians;
	protected final Set<Pedestrian> stepPedestrians;
	private final OSMBehaviorController osmBehaviorController;

	static {
		logger.setDebug();
	}

	public UpdateSchemeParallel(@NotNull final Topography topography) {
		this.topography = topography;
		this.executorService = Executors.newFixedThreadPool(8);
		this.movedPedestrians = new HashSet<>();
		this.stepPedestrians = new HashSet<>();
		this.osmBehaviorController = new OSMBehaviorController();
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		clearStrides(topography);

		movedPedestrians.clear();
		stepPedestrians.clear();
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

	protected void update(@NotNull final PedestrianOSM pedestrian, final double timeStepInSec, final double currentTimeInSec, CallMethod callMethod) {

		// At the moment no other events are supported for the parallel update scheme!
		assert pedestrian.getMostImportantEvent() instanceof ElapsedTimeEvent;
		switch (callMethod) {
			case SEEK:
				updateParallelSeek(pedestrian, currentTimeInSec, timeStepInSec);
				break;
			case RETRY:
				updateParallelSeek(pedestrian, currentTimeInSec,0.0);
			case MOVE:
				updateParallelMove(pedestrian, timeStepInSec);
				break;
			case CONFLICTS:
				updateParallelConflicts(pedestrian);
				break;
			case STEPS:
				updateParallelSteps(pedestrian);
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
	protected void updateParallelSeek(@NotNull final PedestrianOSM pedestrian, final double currentTimeInSec, final double timeStepInSec) {
		if (pedestrian.getTimeOfNextStep() < currentTimeInSec) {
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
	private void  updateParallelMove(@NotNull final PedestrianOSM pedestrian, final double timeStepInSec) {
		if (movedPedestrians.contains(pedestrian)) {
			osmBehaviorController.makeStep(pedestrian, topography, timeStepInSec);
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
				double otherTimeOfNextEvent = ((PedestrianOSM) ped).getTimeOfNextStep();
				double timeOfNextEvent = pedestrian.getTimeOfNextStep();

				if (otherTimeOfNextEvent < timeOfNextEvent) {
					undoStep = true;
					break;
				} else if (otherTimeOfNextEvent == timeOfNextEvent && ped.getId() < pedestrian.getId()) {
					undoStep = true;
					break;
				}
			}

			if (!undoStep) {
				synchronized (stepPedestrians) {
					stepPedestrians.add(pedestrian);
				}
			}
		}
	}

	protected void updateParallelSteps(@NotNull final PedestrianOSM pedestrian) {
		if(movedPedestrians.contains(pedestrian)) {
			if(stepPedestrians.contains(pedestrian)) {
				pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
			} else {
				osmBehaviorController.undoStep(pedestrian, topography);
			}
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
