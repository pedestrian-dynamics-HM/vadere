package org.vadere.simulator.projects.dataprocessing.processors;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.data.Row;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * A ForEachPedestrianPositionProcessor calculate a {@link Row} for every pedestrian position for
 * each pedestrian
 * for every time step.
 * 
 *
 */
public interface ForEachPedestrianPositionProcessor extends Processor {
	Row postUpdate(final SimulationState state, final int pedId, final VPoint position);
}
