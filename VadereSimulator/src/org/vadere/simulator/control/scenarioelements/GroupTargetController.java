package org.vadere.simulator.control.scenarioelements;

import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.models.groups.GroupIterator;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

public class GroupTargetController extends TargetController {

	private final GroupIterator groupIterator;

	public GroupTargetController(Topography topography, Target target, GroupIterator groupIterator) {
		super(topography, target);
		this.groupIterator = groupIterator;
	}

	@Override
	protected boolean checkRemove(Agent agent) {
		if (!super.checkRemove(agent)) {
			while (groupIterator.getGroupIterator().hasNext()) {
				Group group = groupIterator.getGroupIterator().next();
				group.checkNextTarget(target.getNextSpeed());
			}
			return false;
		}
		return true;
	}

}
