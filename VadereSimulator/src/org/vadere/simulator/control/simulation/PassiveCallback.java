package org.vadere.simulator.control.simulation;

import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.projects.Domain;

import java.util.Map;

/**
 * This interface defines a callbacks for the simulation loop.
 * It is called "passive" since it's implementations cannot change the state.
 *
 *
 */
public interface PassiveCallback {
	void preLoop(double simTimeInSec);

	void postLoop(double simTimeInSec);

	void preUpdate(double simTimeInSec);

	void postUpdate(double simTimeInSec);

	void setDomain(Domain scenario);

	default void setPotentialFieldTarget(@Nullable IPotentialFieldTarget potentialFieldTarget){}

	default void setPotentialField(@Nullable IPotentialField potentialField) {}

	default void setPotentialFieldTargetMesh(@Nullable Map<Integer, PMesh> meshMap) {}
}
