package org.vadere.simulator.models.groups;

import org.vadere.state.scenario.Pedestrian;

//wenn ped erzeugt und entfernet werden
public abstract class GroupFactory {
	public abstract int getOpenPersons();

	public abstract void elementAdded(Pedestrian pedestrian);

	public abstract int createNewGroup();
}
