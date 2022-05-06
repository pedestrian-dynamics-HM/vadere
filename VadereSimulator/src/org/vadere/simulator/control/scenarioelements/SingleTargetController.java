package org.vadere.simulator.control.scenarioelements;

import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

public class SingleTargetController extends TargetController {

	public SingleTargetController(Topography topography, Target target) {
		super(topography, target);
	}

	@Override
	protected boolean checkRemove(Agent agent) {
		if (!super.checkRemove(agent)) {
			checkNextTarget(agent, target.getNextSpeed());
			return false;
		}
		return true;
	}

	// TODO [priority=high] [task=deprecation] removing the target from the list is deprecated, but still very frequently used everywhere.
	public void checkNextTarget(Agent agent, double nextSpeed) {
		final int nextTargetListIndex = agent.getNextTargetListIndex();

		// Deprecated target list usage
		if (nextTargetListIndex <= -1 && !agent.getTargets().isEmpty()) {
			agent.getTargets().removeFirst();
		}

		// The right way (later this first check should not be necessary anymore):
		if (agent.hasNextTarget()) {
			agent.incrementNextTargetListIndex();
		}

		// set a new desired speed, if possible. you can model street networks with differing
		// maximal speeds with this.
		if (nextSpeed >= 0) {
			agent.setFreeFlowSpeed(nextSpeed);
		}
	}
	
}
