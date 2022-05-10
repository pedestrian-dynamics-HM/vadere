package org.vadere.simulator.control.psychology.cognition.models;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.models.AttributesPedestrianRepulsionPotentialStrategy;
import org.vadere.state.attributes.models.psychology.cognition.AttributesCognitionModel;
import org.vadere.state.attributes.models.psychology.cognition.AttributesSocialDistancingCognitionModel;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.DistanceRecommendation;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.FootstepHistory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

/**
 * Suppose a distance recommendation (a {@link DistanceRecommendation}) occurred (keyword: social distancing).
 * In this case, agents perform social distancing behavior.
 * The distancing behavior depends on two parameters that are defined in {@link DistanceRecommendation}
 * The parameter socialDistance controls the distancing behavior, see implementation in the locomotion layer.
 * The parameter cloggingTimeAllowedInSecs controls after which time
 * agents ignore the distance recommendation when they have been clogged.
 **/

public class SocialDistancingCognitionModel implements ICognitionModel {

    private HashMap<Pedestrian, Double> cloggingStartTimes = new HashMap<>();
    private AttributesSocialDistancingCognitionModel attributes;

    @Override
    public void initialize(Topography topography, Random random) {
        this.attributes = new AttributesSocialDistancingCognitionModel();
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {

            Stimulus stimulus = pedestrian.getMostImportantStimulus();
            updateCloggingTime(pedestrian);
            if (stimulus instanceof ElapsedTime || ignoreDistanceRecommendationInCaseOfClogging(pedestrian)) {
                pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);
            } else if (stimulus instanceof DistanceRecommendation) {
                pedestrian.setSelfCategory(SelfCategory.SOCIAL_DISTANCING);
                PedestrianOSM ped = (PedestrianOSM) pedestrian;

                AttributesPedestrianRepulsionPotentialStrategy attr = new AttributesPedestrianRepulsionPotentialStrategy();
                attr.setSocialDistanceUpperBound(this.attributes.getMaxDistance());
                attr.setSocialDistanceLowerBound(this.attributes.getMinDistance());
                attr.setPersonalSpaceWidthIntercept(this.attributes.getRepulsionIntercept());
                attr.setPersonalSpaceWidthFactor(this.attributes.getRepulsionFactor());
                ped.setCombinedPotentialStrategyAttributes(attr);
            }
        }
    }

    @Override
    public void setAttributes(AttributesCognitionModel attributes) {
        this.attributes = (AttributesSocialDistancingCognitionModel) attributes;

    }

    @Override
    public AttributesSocialDistancingCognitionModel getAttributes() {
        return this.attributes;
    }


    protected boolean ignoreDistanceRecommendationInCaseOfClogging(Pedestrian pedestrian) {

        Stimulus stimulus = pedestrian.getMostImportantStimulus();
        if (stimulus instanceof DistanceRecommendation) {
            double thresholdTime = ((DistanceRecommendation) stimulus).getCloggingTimeAllowedInSecs();
            double waitingTime = getTimePedIsClogged(pedestrian);
            return waitingTime > thresholdTime;
        }
        return false;
    }

    protected void updateCloggingTime(Pedestrian pedestrian) {

        FootstepHistory footstepHistory = pedestrian.getFootstepHistory();

        double timeClogged = 0.0;
        if (footstepHistory.size() > 0) {
            FootStep fs = footstepHistory.getYoungestFootStep();
            if (fs != null) {
                if (fs.getEnd().distance(fs.getStart()) <= Double.MIN_VALUE) {
                    timeClogged += fs.getEndTime() - fs.getStartTime();
                    if (cloggingStartTimes.containsKey(pedestrian)) {
                        timeClogged += cloggingStartTimes.get(pedestrian);
                    }
                }
            }
        }
        cloggingStartTimes.put(pedestrian, timeClogged);
    }

    protected double getTimePedIsClogged(Pedestrian pedestrian) {
        return cloggingStartTimes.get(pedestrian);
    }


}

