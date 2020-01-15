package org.vadere.simulator.models.potential.combinedPotentials;

import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;

/**
 * Combine potential so that agents are repelled by targets.
 */
public class TargetRepulsionStrategy implements ICombinedPotentialStrategy {
    private IPotentialFieldTarget potentialFieldTarget;
    private PotentialFieldObstacle potentialFieldObstacle;
    private PotentialFieldAgent potentialFieldAgent;

    public TargetRepulsionStrategy(IPotentialFieldTarget potentialFieldTarget, PotentialFieldObstacle potentialFieldObstacle, PotentialFieldAgent potentialFieldAgent) {
        this.potentialFieldTarget = potentialFieldTarget;
        this.potentialFieldObstacle = potentialFieldObstacle;
        this.potentialFieldAgent = potentialFieldAgent;
    }

    @Override
    public double getValue(IPoint newPos, Agent thisAgent, Collection<? extends Agent> otherAgents) {
        double targetPotential = potentialFieldTarget.getPotential(newPos, thisAgent);

        // The target potential is intialized with "Double.MAX_VALUE" in obstacle regions!
        // Multiplying "Double.MAX_VALUE" with "-1" would cause an agent to walk into an obstacle.
        if (targetPotential != Double.MAX_VALUE) {
            targetPotential *= -1;
        }

        double obstaclePotential = potentialFieldObstacle.getObstaclePotential(newPos, thisAgent);
        double agentPotential = potentialFieldAgent.getAgentPotential(newPos, thisAgent, otherAgents);

        return targetPotential + agentPotential + obstaclePotential;
    }
}
