package org.vadere.simulator.models.groups.cgm;

import org.vadere.simulator.models.groups.GroupFactory;
import org.vadere.simulator.models.groups.GroupSizeDeterminator;
import org.vadere.state.scenario.Pedestrian;

import java.util.LinkedList;

public class CentroidGroupFactory extends GroupFactory {

	transient private CentroidGroupModel centroidGroupModel;
	transient private GroupSizeDeterminator groupSizeDeterminator;

	private LinkedList<CentroidGroup> newGroups;

	public CentroidGroupFactory(CentroidGroupModel centroidGroupModel,
								GroupSizeDeterminator groupSizeDet) {
		this.centroidGroupModel = centroidGroupModel;
		this.groupSizeDeterminator = groupSizeDet;
		this.newGroups = new LinkedList<>();
	}

	@Override
	public int getOpenPersons() {
		if (newGroups.peekFirst() == null) {
			throw new IllegalStateException("No empty group exists");
		}

		return newGroups.peekFirst().getOpenPersons();
	}

	private void assignToGroup(Pedestrian ped) {
		CentroidGroup currentGroup = newGroups.peekFirst();
		if (currentGroup == null) {
			throw new IllegalStateException("No empty group exists to add Pedestrian: " + ped.getId());
		}

		currentGroup.addMember(ped);
		ped.addGroupId(currentGroup.getID(), currentGroup.getSize());
		centroidGroupModel.registerMember(ped, currentGroup);
		if (currentGroup.getOpenPersons() == 0) {
			newGroups.pollFirst(); // remove full group from list.
		}
	}

	public int createNewGroup() {
		CentroidGroup newGroup = centroidGroupModel.getNewGroup(groupSizeDeterminator
				.nextGroupSize());
		newGroups.addLast(newGroup);
		return newGroup.getSize();
	}

	//listener methode (aufruf
	public void elementAdded(Pedestrian pedestrian) {
		assignToGroup(pedestrian);
	}

	public void elementRemoved(Pedestrian ped) {
		CentroidGroup group = centroidGroupModel.removeMember(ped);
//		System.out.printf("Remove ped %s from group %s %n", ped.getId(), group != null ? group.getID() : "noGroup");
	}

	public GroupSizeDeterminator getGroupSizeDeterminator() {
		return groupSizeDeterminator;
	}

	public void setGroupSizeDeterminator(GroupSizeDeterminator groupSizeDeterminator) {
		this.groupSizeDeterminator = groupSizeDeterminator;
	}
}
