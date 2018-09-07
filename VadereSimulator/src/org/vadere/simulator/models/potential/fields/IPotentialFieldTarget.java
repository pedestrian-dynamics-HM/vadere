package org.vadere.simulator.models.potential.fields;

import org.vadere.simulator.models.Model;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;


/**
 * A static (needsUpdate returns always false) or dynamic target potential field for all
 * pedestrians i.e. multiple targets. The used target is the current target of the pedestrian,
 * which may change during the simulation.
 *
 * @author Benedikt Zoennchen
 */
public interface  IPotentialFieldTarget extends IPotentialField, Model {

    /**
     * Returns true if the field is dynamic, false otherwise.
     *
     * @return true if the field is dynamic, false otherwise.
     */
    boolean needsUpdate();

	Vector2D getTargetPotentialGradient(final VPoint pos, final Agent ped);

    /**
     * Returns the IPotentialField which is a copy of the current state
     * such that an update (in case of a dynamic potential field) does not effect the returned copy.
     *
     * @return a copy of the current target potential field
     */
    IPotentialField copyFields();
}
