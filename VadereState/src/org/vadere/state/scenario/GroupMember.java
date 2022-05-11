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

    public void setGroupIds(LinkedList<Integer> groupIds) {
        groupAccess.setGroupIds(groupIds, member);
    }
    public LinkedList<Integer> getGroupIds() {
        return groupAccess.getGroupIds(member);
    }
    public LinkedList<Integer> getGroupSizes() {
        return groupAccess.getGroupSizes(member);
    }
    public void setGroupSizes(LinkedList<Integer> sizes) {
        groupAccess.setGroupSizes(sizes, member);
    }
    public List<Pedestrian> getPedGroupMembers() {
        return groupAccess.getPedGroupMembers(member);
    }
    public void setAgentsInGroup(final LinkedList<Pedestrian> agentsInGroup) {
        groupAccess.setAgentsInGroup(agentsInGroup, member);
    }

    public void addGroupId(int groupId) {
        groupAccess.addGroupId(groupId, member);
    }

}
