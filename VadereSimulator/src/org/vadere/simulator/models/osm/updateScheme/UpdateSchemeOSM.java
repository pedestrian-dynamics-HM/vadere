package org.vadere.simulator.models.osm.updateScheme;


import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.opencl.CLParallelOptimalStepsModel;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.scenario.DynamicElementAddListener;
import org.vadere.state.scenario.DynamicElementRemoveListener;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.types.UpdateType;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.opencl.OpenCLException;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;

import java.util.Random;

public interface UpdateSchemeOSM extends DynamicElementRemoveListener<Pedestrian>, DynamicElementAddListener<Pedestrian> {

	enum CallMethod {
		SEEK, MOVE, CONFLICTS, STEPS, RETRY, SEQUENTIAL, EVENT_DRIVEN
	}

	void update(double timeStepInSec, double currentTimeInSec);

	default void clearStrides(@NotNull final Topography topography) {
		/**
		 * strides and foot steps have no influence on the simulation itself, i.e. they are saved to analyse trajectories
		 */
		for(PedestrianOSM pedestrianOSM : topography.getElements(PedestrianOSM.class)) {
			pedestrianOSM.clearStrides();
			pedestrianOSM.clearFootSteps();
		}
	}

	static UpdateSchemeOSM create(@NotNull final UpdateType updateType, @NotNull final Topography topography, final Random random) {
		switch (updateType) {
			case SEQUENTIAL: return new UpdateSchemeSequential(topography);
			case PARALLEL: return new UpdateSchemeParallel(topography);
			case EVENT_DRIVEN: return new UpdateSchemeEventDriven(topography);
			case SHUFFLE: return new UpdateSchemeShuffle(topography, random);
			//TODO: magic number!
			case EVENT_DRIVEN_PARALLEL: return new UpdateSchemeEventDrivenParallel(topography, 1.4);
			default: throw new IllegalArgumentException(updateType + " is not supported.");
		}
	}

	static UpdateSchemeOSM createOpenCLUpdateScheme(
			@NotNull final Topography topography,
			@NotNull final AttributesOSM attributesOSM,
			@NotNull final AttributesFloorField attributesFloorField,
			@NotNull final EikonalSolver targetEikonalSolver,
			@NotNull final EikonalSolver distanceEikonalSolver) {

		try {
			CLParallelOptimalStepsModel clOptimalStepsModel = new CLParallelOptimalStepsModel(
					attributesOSM,
					attributesFloorField,
					new VRectangle(topography.getBounds()),
					targetEikonalSolver,
					distanceEikonalSolver);

			return new UpdateSchemeCLParallel(topography, clOptimalStepsModel);

		} catch (OpenCLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Moves the pedestrian inside the topography. The pedestrian object already has the new
	 * location (Vpoint to) stored within its position attribute. This method only informs the
	 * topography object of the change in state.
	 *
	 * !IMPORTANT! this function must be called ONLY ONCE  for each pedestrian for each position. To
	 * allow preformat selection of a pedestrian the  managing destructure is not idempotent (cannot
	 * be applied multiple time without changing result).
	 *
	 * @param topography	manages simulation data
	 * @param pedestrian	moving pedestrian. This object's position is already set.
	 * @param from			old position
	 * @param to			new position (same as #pedestrian.getPosition())
	 */
	default void movePedestrian(@NotNull final Topography topography, @NotNull final PedestrianOSM pedestrian, @NotNull final VPoint from, @NotNull final VPoint to) {
		pedestrian.setPosition(to);
		synchronized (topography) {
			topography.moveElement(pedestrian, from);
		}
	}

	/**
	 * Prepare move of pedestrian inside the topography. The pedestrian object already has the new
	 * location (Vpoint to) stored within its position attribute. This method only informs the
	 * topography object of the change in state.
	 *
	 * !IMPORTANT! this function calls movePedestrian which must be called ONLY ONCE  for each
	 * pedestrian for each position. To  allow preformat selection of a pedestrian the  managing
	 * destructure is not idempotent (cannot be applied multiple time without changing result).
	 *
	 * @param topography 	manages simulation data
	 * @param pedestrian	moving pedestrian. This object's position is already set.
	 * @param stepDuration		time in seconds used for the step.
	 */
	default void makeStep(@NotNull final Topography topography, @NotNull final PedestrianOSM pedestrian, final double stepDuration) {
		VPoint currentPosition = pedestrian.getPosition();
		VPoint nextPosition = pedestrian.getNextPosition();

		// start time
		double timeOfNextStep = pedestrian.getTimeOfNextStep();

		// end time
		double entTimeOfStep = pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep();

		if (nextPosition.equals(currentPosition)) {
			pedestrian.setTimeCredit(0);
			pedestrian.setVelocity(new Vector2D(0, 0));

		} else {
			pedestrian.setTimeCredit(pedestrian.getTimeCredit() - pedestrian.getDurationNextStep());
			movePedestrian(topography, pedestrian, pedestrian.getPosition(), nextPosition);
			// compute velocity by forward difference
			Vector2D pedVelocity = new Vector2D(nextPosition.x - currentPosition.x, nextPosition.y - currentPosition.y).multiply(1.0 / stepDuration);
			pedestrian.setVelocity(pedVelocity);
		}

		/**
		 * strides and foot steps have no influence on the simulation itself, i.e. they are saved to analyse trajectories
		 */
		pedestrian.getStrides().add(Pair.of(currentPosition.distance(nextPosition), timeOfNextStep));
		pedestrian.getTrajectory().add(new FootStep(currentPosition, nextPosition, timeOfNextStep, entTimeOfStep));
	}

}
