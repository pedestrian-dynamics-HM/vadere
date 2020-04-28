package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.simulator.models.Model;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.function.Function;


/**
 * A static (needsUpdate returns always false) or dynamic target potential field for some
 * agents, i.e. multiple targets: ((x,y), agent) -> potential.
 *
 * @author Benedikt Zoennchen
 */
public interface IPotentialFieldTarget extends IPotentialField, Model {

    /**
     * Returns true if the field is dynamic, false otherwise.
     *
     * @return true if the field is dynamic, false otherwise.
     */
    boolean needsUpdate();

    /**
     * Returns the gradient of the potential field at pos for agent.
     *
     * @param pos position
     * @param agent agent
     * @return the gradient of the potential field at pos for agent
     */
	Vector2D getTargetPotentialGradient(final VPoint pos, final Agent agent);

    /**
     * Returns a copy of the IPotentialField of the current state such that an update
     * (in case of a dynamic potential field) does not effect the returned copy.
     *
     * @return a copy of the current target potential field
     */
    IPotentialField getSolution();

	Function<Agent, IMesh<?, ?, ?>> getDiscretization();

	double getPotential(@NotNull final IPoint pos, final int targetId);
}
