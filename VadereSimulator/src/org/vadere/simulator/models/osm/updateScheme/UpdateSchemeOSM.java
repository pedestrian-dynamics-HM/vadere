package org.vadere.simulator.models.osm.updateScheme;


import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.opencl.CLOptimalStepsModel;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.scenario.DynamicElementAddListener;
import org.vadere.state.scenario.DynamicElementRemoveListener;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.UpdateType;
import org.vadere.geometry.Vector2D;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VRectangle;
import org.vadere.geometry.opencl.OpenCLException;
import org.vadere.util.potential.calculators.EikonalSolver;

import java.util.Random;

public interface UpdateSchemeOSM extends DynamicElementRemoveListener<Pedestrian>, DynamicElementAddListener<Pedestrian> {

	enum CallMethod {
		SEEK, MOVE, CONFLICTS, STEPS, RETRY, SEQUENTIAL, EVENT_DRIVEN
	}

	void update(double timeStepInSec, double currentTimeInSec);

	static UpdateSchemeOSM create(@NotNull final UpdateType updateType, @NotNull final Topography topography, final Random random) {
		switch (updateType) {
			case SEQUENTIAL: return new UpdateSchemeSequential(topography);
			case PARALLEL: return new UpdateSchemeParallel(topography);
			case EVENT_DRIVEN: return new UpdateSchemeEventDriven(topography);
			case SHUFFLE: return new UpdateSchemeShuffle(topography, random);
			//TODO: magic number!
			case EVENT_DRIVEN_PARALLEL: return new UpdateSchemeEventDrivenParallel(topography, 0.6);
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
			CLOptimalStepsModel clOptimalStepsModel = new CLOptimalStepsModel(
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

	default void movePedestrian(@NotNull final Topography topography, @NotNull final PedestrianOSM pedestrian, @NotNull final VPoint from, @NotNull final VPoint to) {
		pedestrian.setPosition(to);
		synchronized (topography) {
			topography.moveElement(pedestrian, from);
		}
	}

	default void makeStep(@NotNull final Topography topography, @NotNull final PedestrianOSM pedestrian, final double stepTime) {
		VPoint currentPosition = pedestrian.getPosition();
		VPoint nextPosition = pedestrian.getNextPosition();
		if (nextPosition.equals(currentPosition)) {
			pedestrian.setTimeCredit(0);
			pedestrian.setVelocity(new Vector2D(0, 0));

		} else {
			pedestrian.setTimeCredit(pedestrian.getTimeCredit() - pedestrian.getDurationNextStep());
			movePedestrian(topography, pedestrian, pedestrian.getPosition(), nextPosition);
			// compute velocity by forward difference
			Vector2D pedVelocity = new Vector2D(nextPosition.x - currentPosition.x, nextPosition.y - currentPosition.y).multiply(1.0 / stepTime);
			pedestrian.setVelocity(pedVelocity);
		}

		pedestrian.getStrides().add(Pair.of(currentPosition.distance(nextPosition), pedestrian.getTimeOfNextStep()));
	}

}
