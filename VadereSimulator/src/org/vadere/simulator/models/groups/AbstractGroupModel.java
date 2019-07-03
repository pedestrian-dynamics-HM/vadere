package org.vadere.simulator.models.groups;

import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;

public abstract class AbstractGroupModel<T extends Group> implements GroupModel<CentroidGroup> {

	/**
	 * Register a Pedestrian to the specified group. The function does not check if the pedestrian
	 * is already a member of another group. The caller must make sure of that.
	 */
	abstract protected void registerMember(Pedestrian ped, T currentGroup);


	abstract protected T getNewGroup(int size);

	abstract protected T getNewGroup(final int id, final int size);

	abstract protected void assignToGroup(Pedestrian pedestrian);
}
