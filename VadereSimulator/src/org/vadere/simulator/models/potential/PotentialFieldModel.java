package org.vadere.simulator.models.potential;

import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;

public interface PotentialFieldModel {
	IPotentialFieldTarget getPotentialFieldTarget();
	PotentialFieldObstacle getPotentialFieldObstacle();
	PotentialFieldAgent getPotentialFieldAgent();
}
