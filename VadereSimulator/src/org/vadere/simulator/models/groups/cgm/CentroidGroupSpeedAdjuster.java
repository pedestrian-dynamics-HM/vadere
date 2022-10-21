package org.vadere.simulator.models.groups.cgm;

import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Pedestrian;

import java.util.List;


/**
 The adjustment of speeds is based on the concept presented in seitz-2014 (doi: 10.1007/978-3-319-02447-9_67).
 Group members ahead slow down and wait for others. Group members behind increase their speed to catch up.
 However, the seitz-2014 model did not consider that group members cannot always wait, e.g. when a corridor is very busy.

 In such cases, the seitz-2014 model can produce unrealistic blockades:
 group members cannot catch up when there are people from other groups are already standing in the way.
 Since they cannot catch up, their group members ahead slow down and start waiting.
 This results in even more people blocking each other, which can lead to a complete congestion.

 In reality, group members would not wait in such a situation.
 Depending on the flow situation, they would move on and try to find the lost member later.

 For this reason, the model of seitz-2014 was extended in the context of a bachelor thesis (hertle-2022).
 The basic principle is that group members can temporarily get lost and act as individuals.
 Agents ahead slow down, but they do no longer wait if they are too far apart.
 In this case, they move on and act as individuals.
 A similar behavior was implemented for agents behind.
 This behavior is optional and can be controlled by AttributesCGM attributes.
 **/





public class CentroidGroupSpeedAdjuster implements SpeedAdjuster {

    private final CentroidGroupModel groupCollection;

    public CentroidGroupSpeedAdjuster(CentroidGroupModel groupCollection) {
        this.groupCollection = groupCollection;
    }

    @Override
    public double getAdjustedSpeed(Pedestrian ped, double originalSpeed) {
        double result = 1.0;
        double aheadDistance = 0;

        CentroidGroup group = groupCollection.getGroup(ped);

        if (group != null) {

            if (group.isLostMember(ped)) {
                group.reevaluateLostMember(ped);
                // wait behaviour
                if (ped instanceof PedestrianOSM) {
                    if (((PedestrianOSM) ped).getRelevantPedestrians().size()
                            < groupCollection.getAttributesCGM().getWaitBehaviourRelevantAgentsFactor()) {
                        group.wakeFromLostMember(ped);
                        if (group.isGroupTarget(ped.getNextTargetId()) &&
                                group.getRelativeDistanceCentroid(ped, false, true) > 7) {
                            result = Double.MIN_VALUE;
                        }
                        group.setLostMember(ped); //TODO: [priority=low] [task=refactoring]  do not set lost members in the speed adjuster
                    }
                }
            }
            if (!group.isLostMember(ped)) {
                aheadDistance = group.getRelativeDistanceCentroid(ped, false, true);

                // TODO [priority=low] [task=refactoring] move Parameters to AttributesCGM
                // equations taken from 'Pedestrian Group Behavior in a Cellular Automaton'
                // BibTex-Key: seitz-2014
                // formular not completely the smae to seitz-2014 line 34 is  8/(delta +15) and not 8/(delta + 17)
                if (aheadDistance > 8) {
                    if (!group.isCentroidWithinObstacle()) {
                        result = Double.MIN_VALUE; // wait behavior
                        if (groupCollection.getAttributesCGM().isLostMembers()) {
                            group.setLostMember(ped);  //TODO: [priority=low] [task=refactoring]  do not set lost members in the speed adjuster
                        }
                    }
                    // else: do not wait, treat group member as "lost"
                    // holds if agents get seperated by obstacles broader 8[m]
                } else if (aheadDistance >= 1) {
                    result /= 1.0 + aheadDistance / 8 - 1 / 8 + 1;
                } else if (aheadDistance >= 0) {
                    result /= 1.0 + Math.pow(aheadDistance, 2);
                } else if (aheadDistance >= -1) {
                    result /= 0.75;
                } else if (aheadDistance > -8) {
                    result /= 0.65;
                } else {
                    result /= 0.65;
                        if (groupCollection.getAttributesCGM().isLostMembers()) {
                            if (!group.isCentroidWithinObstacle()) {
                            group.setLostMember(ped);  //TODO: [priority=low] [task=refactoring]  do not set lost members in the speed adjuster
                        }
                    }
                }
            }
        }

        return originalSpeed * result;
    }
}
