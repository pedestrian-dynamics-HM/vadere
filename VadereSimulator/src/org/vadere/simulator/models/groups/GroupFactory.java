package org.vadere.simulator.models.groups;

import org.vadere.state.scenario.DynamicElementAddListener;
import org.vadere.state.scenario.DynamicElementRemoveListener;
import org.vadere.state.scenario.Pedestrian;

//wenn ped erzeugt und entfernet werden
public abstract class GroupFactory
		implements DynamicElementAddListener<Pedestrian>, DynamicElementRemoveListener<Pedestrian>{
	public abstract int getOpenPersons();

	@Override
	public abstract void elementAdded(Pedestrian element);

	@Override
	public abstract void elementRemoved(Pedestrian element);
}
