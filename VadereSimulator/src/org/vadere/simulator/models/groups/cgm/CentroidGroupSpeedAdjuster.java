package org.vadere.simulator.models.groups.cgm;

import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Pedestrian;

import java.util.List;


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
                if (ped instanceof PedestrianOSM) {
                    if (((PedestrianOSM) ped).getRelevantPedestrians().size() < 4) {
                        if (group.getRelativeDistanceCentroid(ped) > 8) {
                            result = Double.MIN_VALUE;
                        }
                    }
                }
            }
            if (!group.isLostMember(ped)) {
                aheadDistance = group.getRelativeDistanceCentroid(ped);

                // TODO [priority=low] [task=refactoring] move Parameters to AttributesCGM
                // equations taken from 'Pedestrian Group Behavior in a Cellular Automaton'
                // BibTex-Key: seitz-2014
                // formular not completely the smae to seitz-2014 line 34 is  8/(delta +15) and not 8/(delta + 17)
                if (aheadDistance > 8) {
                    if (!group.isCentroidWithinObstacle()) {
                        result = Double.MIN_VALUE; // wait behavior
                        if (groupCollection.getAttributesCGM().isLostMembers()) {
                            group.setLostMember(ped);
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
                            group.setLostMember(ped);
                        }
                    }
                }
            }
        }

        return originalSpeed * result;
    }
}
