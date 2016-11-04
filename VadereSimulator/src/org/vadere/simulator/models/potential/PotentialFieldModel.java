package org.vadere.simulator.models.potential;

import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.models.potential.fields.PotentialFieldTarget;

public interface PotentialFieldModel {
	PotentialFieldTarget getPotentialFieldTarget();
	PotentialFieldObstacle getPotentialFieldObstacle();
	PotentialFieldAgent getPotentialFieldAgent();
}
