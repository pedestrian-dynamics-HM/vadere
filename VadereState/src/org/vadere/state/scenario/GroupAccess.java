package org.vadere.state.scenario;

import org.vadere.state.scenario.Pedestrian;

import java.util.LinkedList;
import java.util.List;

public interface GroupAccess {
    void setGroupIds(List<Integer> groupIds, Pedestrian ped);
    List<Integer> getGroupIds(Pedestrian ped);
    List<Integer> getGroupSizes(Pedestrian ped);
    void setGroupSizes(LinkedList<Integer> groupSizes, Pedestrian ped);
    List<Pedestrian> getPedGroupMembers(Pedestrian ped);
    void setAgentsInGroup(final List<Pedestrian> agentsInGroup, Pedestrian ped);
}
