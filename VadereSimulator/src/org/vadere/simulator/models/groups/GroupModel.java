package org.vadere.simulator.models.groups;

import org.vadere.simulator.models.Model;
import org.vadere.state.scenario.ScenarioElement;

public interface GroupModel extends Model {
	public Group getGroup(ScenarioElement ped);

	public void registerMember(ScenarioElement ped, Group currentGroup);

	public CentroidGroup removeMember(ScenarioElement ped);

	public Group getNewGroup(int size);

	public GroupFactory getGroupFactory(int SourceId);
}
