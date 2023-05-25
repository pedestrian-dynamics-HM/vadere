package org.vadere.simulator.models.potential;

import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;

public interface PotentialFieldModel {
  IPotentialFieldTarget getPotentialFieldTarget();

  PotentialFieldObstacle getPotentialFieldObstacle();

  PotentialFieldAgent getPotentialFieldAgent();
}
