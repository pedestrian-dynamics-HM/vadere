package org.vadere.simulator.models.potential.combinedPotentials;

import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;

/**
 * Combine potential so that agents are attracted by targets
 * but other agents are less weighted.
 */
public class TargetAttractionAndEvasionStrategy implements ICombinedPotentialStrategy {
    private IPotentialFieldTarget potentialFieldTarget;
    private PotentialFieldObstacle potentialFieldObstacle;
    private PotentialFieldAgent potentialFieldAgent;

    public TargetAttractionAndEvasionStrategy(IPotentialFieldTarget potentialFieldTarget, PotentialFieldObstacle potentialFieldObstacle, PotentialFieldAgent potentialFieldAgent) {
        this.potentialFieldTarget = potentialFieldTarget;
        this.potentialFieldObstacle = potentialFieldObstacle;
        this.potentialFieldAgent = potentialFieldAgent;
    }

    @Override
    public double getValue(IPoint newPos, Agent thisAgent, Collection<? extends Agent> otherAgents) {
        double targetPotential = potentialFieldTarget.getPotential(newPos, thisAgent);
        double obstaclePotential = potentialFieldObstacle.getObstaclePotential(newPos, thisAgent);
        double agentPotential = potentialFieldAgent.getAgentPotential(newPos, thisAgent, otherAgents);

        agentPotential = agentPotential * 0.8;

        return targetPotential + agentPotential + obstaclePotential;
    }
}
