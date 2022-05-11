package org.vadere.state.scenario;

import java.util.LinkedList;
import java.util.List;

public class GroupMember {

    private final Pedestrian member;
    private final GroupAccess groupAccess;

    public GroupMember(Pedestrian member, GroupAccess groupAccess) {
        this.member = member;
        this.groupAccess = groupAccess;
    }

    void setGroupIds(LinkedList<Integer> groupIds) {
        groupAccess.setGroupIds(groupIds, member);
    };
    List<Integer> getGroupIds() {
        return groupAccess.getGroupIds(member);
    };
    List<Integer> getGroupSizes() {
        return groupAccess.getGroupSizes(member);
    };
    void setGroupSizes(LinkedList<Integer> sizes) {
        groupAccess.setGroupSizes(sizes, member);
    }
    List<Pedestrian> getPedGroupMembers() {
        return groupAccess.getPedGroupMembers(member);
    };
    void setAgentsInGroup(final LinkedList<Pedestrian> agentsInGroup) {
        groupAccess.setAgentsInGroup(agentsInGroup, member);
    };


}
