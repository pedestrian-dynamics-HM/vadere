package org.vadere.simulator.models.bhm.helpers.targetdirection;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.state.scenario.Target;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * Computes the target direction for a target which is defined by exactly one shape using the Euclidean distance.
 * This will NOT incorporate any obstacles between the current pedestrian position and the target shape.
 *
 * @author Benedikt Zoennchen
 */
public class TargetDirectionEuclidean implements TargetDirection {
	private PedestrianBHM pedestrianBHM;

	public TargetDirectionEuclidean(@NotNull final PedestrianBHM pedestrianBHM) {
		this.pedestrianBHM = pedestrianBHM;
	}

	public VPoint getTargetDirection(final Target target) {
		VPoint position = pedestrianBHM.getPosition();
		return target.getShape().closestPoint(pedestrianBHM.getPosition()).subtract(position).norm();
	}
}
