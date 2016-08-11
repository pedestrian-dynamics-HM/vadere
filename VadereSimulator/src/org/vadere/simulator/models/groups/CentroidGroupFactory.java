package org.vadere.simulator.models.groups;

import org.vadere.state.scenario.Pedestrian;

public class CentroidGroupFactory implements GroupFactory {

	private CentroidGroupModel groupCollection;
	private GroupSizeDeterminator groupSizeDeterminator;

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
				.getGroupSize());
	}

	@Override
	public void elementAdded(Pedestrian pedestrian) {
		if (requiresNewGroup()) {
			createNewGroup();
		}
		assignToGroup(pedestrian);
	}
}
