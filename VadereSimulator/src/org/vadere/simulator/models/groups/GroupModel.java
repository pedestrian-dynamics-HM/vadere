package org.vadere.simulator.models.groups;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.state.scenario.ScenarioElement;

import java.util.List;

public interface GroupModel extends Model {
	public Group getGroup(ScenarioElement ped);

	public void registerMember(ScenarioElement ped, Group currentGroup);

	public CentroidGroup removeMember(ScenarioElement ped);

	public Group getNewGroup(int size);

	public GroupFactory getGroupFactory(int sourceId);

	public void initializeGroupFactory(int sourceId, List<Double> groupSizeDistribution);
}
