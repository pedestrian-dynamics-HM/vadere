package org.vadere.simulator.models.potential.combinedPotentials;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianCompactSoftshell;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.state.attributes.models.AttributesPedestrianRepulsionPotentialStrategy;
import org.vadere.state.psychology.perception.types.DistanceRecommendation;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;

/**
 * Agents are repelled by other agents by increasing the potentials of neighboring agents.
 * Therefore, the parameters of the respective potential functions
 * need to be adjusted depending on the required social distance.
 * The underlying formulas can be found in the article Mayr, Koester: Social distancing with the Optimal Steps Model
 * We implement the realistic use case only.
 **/

public class PedestrianRepulsionStrategy implements ICombinedPotentialStrategy {
    private IPotentialFieldTarget potentialFieldTarget;
    private PotentialFieldObstacle potentialFieldObstacle;
    private PotentialFieldAgent potentialFieldAgent;
    private AttributesPedestrianRepulsionPotentialStrategy attributes;

    public PedestrianRepulsionStrategy(IPotentialFieldTarget potentialFieldTarget, PotentialFieldObstacle potentialFieldObstacle, PotentialFieldAgent potentialFieldAgent, AttributesPedestrianRepulsionPotentialStrategy attributes) {
        this.potentialFieldTarget = potentialFieldTarget;
        this.potentialFieldObstacle = potentialFieldObstacle;
        this.potentialFieldAgent = potentialFieldAgent;
        this.attributes = attributes;
     }

    public void setAttributes(AttributesPedestrianRepulsionPotentialStrategy attributes) {
        this.attributes = attributes;
    }

    @Override
    public double getValue(IPoint pos, Agent pedestrian, Collection<? extends Agent> otherAgents) {

        double targetPotential = potentialFieldTarget.getPotential(pos, pedestrian);
        double obstaclePotential = potentialFieldObstacle.getObstaclePotential(pos, pedestrian);
        double socialDistance = getSocialDistance((PedestrianOSM) pedestrian);

        double agentPotential = 0.0;
        if (isSocialDistanceInRange(socialDistance)){
            agentPotential = getSocialDistancingAgentPotential(pos, pedestrian, otherAgents, socialDistance);
        } else {
            throw new RuntimeException("Social distance must be in range [1.25, 2.0]. Got " + socialDistance);
        }
        return targetPotential + agentPotential + obstaclePotential;
    }

    private double getSocialDistancingAgentPotential(final IPoint pos, final Agent pedestrian, final Collection<? extends Agent> otherAgents, final double socialDistance) {
        double agentPotential = 0.0;
        for (Agent neighbor : otherAgents) {
            if (neighbor.getId() != pedestrian.getId()) {
                double height = getHeightFromSocialDistance(socialDistance);
                double personalSpaceWidth = getPersonalSpaceFromSocialDistance(socialDistance);
                agentPotential +=((PotentialFieldPedestrianCompactSoftshell) potentialFieldAgent)
                        .getAgentPotential(pos, pedestrian, neighbor, height, personalSpaceWidth);
            }
        }
        return agentPotential;
    }

    private double getSocialDistance(final PedestrianOSM pedestrian) {
        Stimulus stimulus = pedestrian.getMostImportantStimulus();
        double socialDistance = 0.0;
        if (stimulus instanceof DistanceRecommendation){
            socialDistance = ((DistanceRecommendation) stimulus).getSocialDistance();
        }
        return socialDistance;
    }


    /**
     * Compute the personal space width depending on the social distance for the realistic use case.
     * Original equation:
     * Personal space width w = 1.6444d − 0.0658c − 0.6161
     * where d is the desired social distance and c is the corridor with.
     * We use a fixed corridor width c=2 in this implementation.
     * **/
    private double getPersonalSpaceFromSocialDistance(double socialDistance) {
        return getAttributes().getPersonalSpaceWidthFactor() *socialDistance - getAttributes().getPersonalSpaceWidthIntercept();
    }

    /**
     * To compute the personal space width according to the above equation,
     * the height must be set to 850.
     * */
    private double getHeightFromSocialDistance(double socialDistance) {
        return attributes.getPersonalSpaceStrength();
    }

    private boolean isSocialDistanceInRange(double socialDistance) {
        // additional check (already checked in ScenarioChecker!)
        return (socialDistance >= getAttributes().getSocialDistanceLowerBound() && socialDistance <= getAttributes().getSocialDistanceUpperBound());
    }

    public AttributesPedestrianRepulsionPotentialStrategy getAttributes() {
        return attributes;
    }
}
