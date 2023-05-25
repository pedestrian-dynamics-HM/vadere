package org.vadere.simulator.models.potential.combinedPotentials;

import java.util.Collection;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * In "seitz-2012", the total potential for some point x (in R²) is the combination of different
 * potentials:
 *
 * <p>combined(x) = target(x) + obstacles(x) + agents(x)
 *
 * <p>Use this interface to implement different combinations of these potentials. For instance,
 * negate the target potential so that agents run away from targets:
 *
 * <p>combined(x) = -target(x) + obstacles(x) + agents(x)
 *
 * <p>The implementing classes require objects of following interfaces to calculate the combined
 * potential: - {@link org.vadere.simulator.models.potential.fields.IPotentialFieldTarget} - {@link
 * org.vadere.simulator.models.potential.fields.PotentialFieldObstacle} - {@link
 * org.vadere.simulator.models.potential.fields.PotentialFieldAgent}
 *
 * <p>At runtime, implementations of this interface can be replaced by using the strategy pattern.
 */
public interface ICombinedPotentialStrategy {
  /** Get the combined potential at given position. */
  public double getValue(IPoint newPos, Agent thisAgent, Collection<? extends Agent> otherAgents);
}
