package org.vadere.simulator.models.bhm.helpers.targetdirection;

import org.vadere.state.scenario.Target;
import org.vadere.util.geometry.shapes.VPoint;

public interface TargetDirection {
	VPoint getTargetDirection(final Target target);
}
