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
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.CollectionUtils;
import org.vadere.util.logging.Logger;

/**
 * The implementation of the parallel update scheme described in 'How update schemes influence crowd simulations' i.e. seitz-2014b.
 * The implementation is not based on the time credit concept but on event times similar to the event driven update scheme.
 * The difference is that all events which starts within a time span of <tt>currentTimeInSec</tt> - <tt>timeStepInSec</tt> and <tt>currentTimeInSec</tt>
 * will be performed in parallel on the bases of the situation (i.e. agents position) at <tt>currentTimeInSec</tt> - <tt>timeStepInSec</tt>.
 */
public class UpdateSchemeParallel implements UpdateSchemeOSM {
	private static final int NUMBER_OF_THREADS = 8;
	private static Logger logger = Logger.getLogger(UpdateSchemeParallel.class);
	protected final ExecutorService executorService;
	protected final Topography topography;

	/**
	 * marks an agent that will move in the time span.
	 */
	protected final Set<Pedestrian> movePedestrians;

	/**
	 * marks an agent which shall move back because of conflicts.
	 */
	protected final Set<Pedestrian> undoPedestrians;

	private final OSMBehaviorController osmBehaviorController;

	static {
		logger.setDebug();
	}

	public UpdateSchemeParallel(@NotNull final Topography topography) {
		this.topography = topography;
		this.executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		this.movePedestrians = new HashSet<>();
		this.undoPedestrians = new HashSet<>();
		this.osmBehaviorController = new OSMBehaviorController();
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		clearStrides(topography);

		do {
			movePedestrians.clear();
			undoPedestrians.clear();
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
		} while (!movePedestrians.isEmpty());
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
		assert pedestrian.getMostImportantStimulus() instanceof ElapsedTime && pedestrian.getSelfCategory() == SelfCategory.TARGET_ORIENTED;
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
		if (pedestrian.getTimeOfNextStep() == Pedestrian.INVALID_NEXT_EVENT_TIME) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
			return;
		}

		if (pedestrian.getTimeOfNextStep() < currentTimeInSec) {
			pedestrian.updateNextPosition();
			synchronized (movePedestrians) {
				movePedestrians.add(pedestrian);
			}
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
		if (movePedestrians.contains(pedestrian)) {
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
		if (movePedestrians.contains(pedestrian)) {
			pedestrian.refreshRelevantPedestrians();
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
				synchronized (undoPedestrians) {
					undoPedestrians.add(pedestrian);
				}
			}
		}
	}

	protected void updateParallelSteps(@NotNull final PedestrianOSM pedestrian) {
		if(movePedestrians.contains(pedestrian)) {
			if(undoPedestrians.contains(pedestrian)) {
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

	@Override
	public void shutdown() {
		executorService.shutdown();
	}
}
