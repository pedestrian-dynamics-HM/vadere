package org.vadere.simulator.control.psychology.cognition.models;

import org.vadere.simulator.utils.topography.TopographyHelper;
import org.vadere.state.attributes.models.psychology.cognition.AttributesCognitionModel;
import org.vadere.state.attributes.models.psychology.cognition.AttributesThreatCognitionModel;
import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.Threat;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Suppose a threat (a {@link Threat}) occurred.
 *
 * Check following conditions for a pedestrian:
 * <ol>
 *     <li>Is pedestrian inside threat area.</li>
 *     <li>Is pedestrian outside threat area.</li>
 *     <li>If pedestrian is outside threat area, test if other pedestrians are nearby
 *     who have perceived the threat. If so, imitate their behavior if they are in-group members.</li>
 * </ol>
 */
public class ThreatCognitionModel implements ICognitionModel {

    private Topography topography;
    private AttributesThreatCognitionModel attributes;

    @Override
    public void initialize(Topography topography, Random random) {
        this.topography = topography;
        this.attributes = new AttributesThreatCognitionModel();
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {
            if (pedestrian.getMostImportantStimulus() instanceof Threat) {
                handleThreat(pedestrian, pedestrian.getMostImportantStimulus());
            } else if (pedestrian.getMostImportantStimulus() instanceof ElapsedTime) {
                handleElapsedTime(pedestrian);
            } else {
                throw new IllegalArgumentException("Can only process \"Threat\" and \"ElapsedTime\" stimuli!");
            }

        }
    }

    @Override
    public void setAttributes(AttributesCognitionModel attributes) {
        this.attributes = (AttributesThreatCognitionModel) attributes;
    }

    @Override
    public AttributesThreatCognitionModel getAttributes() {
        return this.attributes;
    }

    private void handleThreat(Pedestrian pedestrian, Stimulus stimulus) {
        if (isNewThreatForPedestrian(pedestrian, (Threat) stimulus)) {
            pedestrian.getThreatMemory().setLatestThreatUnhandled(true);
        }

        // Current stimulus is a threat => store it and make clear that pedestrian is inside threat area.
        pedestrian.getThreatMemory().add((Threat) stimulus);
        pedestrian.setSelfCategory(SelfCategory.THREATENED);

        // Gerta suggests to apply SelfCategory.COMMON_FATE
        // so that agents directly search a safe zone if they are blocked by a wall.
        if (TopographyHelper.pedestrianIsBlockedByObstacle(pedestrian, topography)) {
            pedestrian.setSelfCategory(SelfCategory.COMMON_FATE);
        }
    }

    private boolean isNewThreatForPedestrian(Pedestrian pedestrian, Threat threat) {
        boolean isNewThreat = false;

        if (pedestrian.getThreatMemory().isEmpty()) {
            isNewThreat = true;
        } else {
            // Check if pedestrian re-entered the same threat area.
            Threat oldThreat = pedestrian.getThreatMemory().getLatestThreat();
            isNewThreat = oldThreat.getOriginAsTargetId() != threat.getOriginAsTargetId();
        }

        return isNewThreat;
    }

    private void handleElapsedTime(Pedestrian pedestrian) {
        Threat latestThreat = pedestrian.getThreatMemory().getLatestThreat();

        if (latestThreat != null) {
            testIfInsideOrOutsideThreatArea(pedestrian, latestThreat);
        } else { // These agents did not perceive a threat but are aware of other threatened agents.

            if (pedestrian.getGroupMembership() == GroupMembership.OUT_GROUP) {
                pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);
            } else if (pedestrian.getGroupMembership() == GroupMembership.IN_GROUP) {
                imitateThreatenedNeighborIfPresent(pedestrian);
            } else {
                throw new IllegalArgumentException("Can only process \"IN_GROUP\" and \"OUT_GROUP\" group membership!");
            }
        }
    }

    private void testIfInsideOrOutsideThreatArea(Pedestrian pedestrian, Threat latestThreat) {
        VPoint threatOrigin = topography.getTarget(latestThreat.getOriginAsTargetId()).getShape().getCentroid();
        double distanceToThreat = threatOrigin.distance(pedestrian.getPosition());

        boolean pedestrianIsInsideThreatArea = true; // (distanceToThreat <= latestThreat.getRadius());
        boolean pedestrianIsBlockedByObstacle = TopographyHelper.pedestrianIsBlockedByObstacle(pedestrian, topography);

        // Gerta suggests to apply SelfCategory.COMMON_FATE
        // so that agents directly search a safe zone if they are blocked by a wall.
        if (pedestrianIsInsideThreatArea && pedestrianIsBlockedByObstacle == false) {
            pedestrian.setSelfCategory(SelfCategory.THREATENED);
        } else {
            pedestrian.setSelfCategory(SelfCategory.COMMON_FATE);
        }
    }


    /**
     * If a threatened ingroup pedestrian is nearby, use the same reaction as if
     * the current "pedestrian" would have perceived the same threat. I.e, imitate
     * the behavior of the perceived and threatened ingroup member:
     *
     * <ol>
     *     <li>Firstly, accelerate and get out of threat area.</li>
     *     <li>Then, search for a safe zone.</li>
     * </ol>
     *
     * This behavior is triggered by method {@link #handleThreat(Pedestrian, Stimulus)}.
     */
    private void imitateThreatenedNeighborIfPresent(Pedestrian pedestrian) {
        List<Pedestrian> threatenedNeighbors = TopographyHelper.getNeighborsWithSelfCategory(pedestrian, SelfCategory.COMMON_FATE, topography);
        List<Pedestrian> threatenedIngroupNeighbors = threatenedNeighbors.stream()
                .filter(ped -> ped.getGroupMembership() == GroupMembership.IN_GROUP)
                .collect(Collectors.toList());

        if (threatenedIngroupNeighbors.isEmpty() == false) {
            Pedestrian threatenedNeighbor = threatenedIngroupNeighbors.get(0);
            Threat latestThreat = threatenedNeighbor.getThreatMemory().getLatestThreat();

            assert latestThreat != null;

            handleThreat(pedestrian, latestThreat);
        } else {
            pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);
        }
    }

}
