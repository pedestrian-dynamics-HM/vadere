package org.vadere.simulator.models.potential.fields;

import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
@FunctionalInterface
public interface IPotentialField {

    /**
     * Returns a potential at pos for the agent. This can be any potential.
     *
     * @param pos   the position for which the potential will be evaluated
     * @param agent the agent for which the potential will be evaluated
     * @return a potential at pos for the agent
     */
    double getPotential(final VPoint pos, final Agent agent);
}
