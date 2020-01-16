package org.vadere.simulator.control.psychology.cognition;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Bang;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;

/**
 * Suppose a threat (a {@link Bang}) occurred.
 *
 * Check following conditions for a pedestrian:
 * <ol>
 *     <li>Is pedestrian inside threat area.</li>
 *     <li>Is pedestrian outside threat area.</li>
 *     <li>If pedestrian outside threat area, test if other pedestrians are nearby
 *     who have perceived the threat. If so, imitate their behavior if they are ingroup members.</li>
 * </ol>
 */
public class ThreatCognitionModel implements ICognitionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {
            if (pedestrian.getMostImportantStimulus() instanceof Bang) {

                pedestrian.setPerceivedThreat(pedestrian.getMostImportantStimulus());
                pedestrian.setSelfCategory(SelfCategory.INSIDE_THREAT_AREA);
            } else if (pedestrian.getMostImportantStimulus() instanceof ElapsedTime) {

                if (pedestrian.getPerceivedThreat() != null) {
                    if (pedIsInsideThreatArea(pedestrian, (Bang) pedestrian.getPerceivedThreat())) {
                        pedestrian.setSelfCategory(SelfCategory.INSIDE_THREAT_AREA);
                    } else {
                        pedestrian.setSelfCategory(SelfCategory.OUTSIDE_THREAT_AREA);
                    }
                } else {
                    // TODO: Imitate behavior if recognizing an in-group member.
                    //   Maybe, use also cooperative behavior from "CooperativeCognitionModel".
                    pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);
                }

            } else {
                throw new IllegalArgumentException("Use only \"Bang\" and \"ElapsedTime\" stimuli!");
            }
        }
    }

    private boolean pedIsInsideThreatArea(Pedestrian pedestrian, Bang bang) {
        VPoint bangOrigin = topography.getTarget(bang.getOriginAsTargetId()).getShape().getCentroid();
        double distanceToBang = bangOrigin.distance(pedestrian.getPosition());

        boolean isInsideThreatArea = (distanceToBang <= bang.getRadius());

        return isInsideThreatArea;
    }
}
