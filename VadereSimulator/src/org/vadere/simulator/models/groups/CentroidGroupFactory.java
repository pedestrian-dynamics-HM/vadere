package org.vadere.simulator.models.groups;

import org.vadere.state.scenario.Pedestrian;

public class CentroidGroupFactory extends GroupFactory {

	transient private CentroidGroupModel groupCollection;
	transient private GroupSizeDeterminator groupSizeDeterminator;

	private CentroidGroup currentGroup;

	public CentroidGroupFactory(CentroidGroupModel groupCollection,
								GroupSizeDeterminator groupSizeDet) {
		this.groupCollection = groupCollection;
		this.groupSizeDeterminator = groupSizeDet;
	}

	@Override
	public int getOpenPersons() {
		return currentGroup.getOpenPersons();
	}

	private void assignToGroup(Pedestrian ped) {
		currentGroup.addMember(ped);
		ped.addGroupId(currentGroup.getID());
		groupCollection.registerMember(ped, currentGroup);
	}

	private boolean requiresNewGroup() {
		boolean result;

		if (currentGroup == null) {
			result = true;
		} else {
			result = currentGroup.isFull();
		}

		return result;
	}

	private void createNewGroup() {
		currentGroup = groupCollection.getNewGroup(groupSizeDeterminator
				.nextGroupSize());
	}

	//listener methode (aufruf
	public void elementAdded(Pedestrian pedestrian) {
		if (requiresNewGroup()) {
			createNewGroup();
		}
		assignToGroup(pedestrian);
	}

	public void elementRemoved(Pedestrian ped) {
		CentroidGroup group = groupCollection.removeMember(ped);
//		System.out.printf("Remove ped %s from group %s %n", ped.getId(), group != null ? group.getID() : "noGroup");
	}
}
