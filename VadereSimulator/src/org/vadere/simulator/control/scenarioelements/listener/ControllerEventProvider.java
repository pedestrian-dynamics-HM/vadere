package org.vadere.simulator.control.scenarioelements.listener;

import org.vadere.simulator.control.scenarioelements.ScenarioElementController;

public interface ControllerEventProvider  <E, C extends ScenarioElementController> {

	void register(ControllerEventListener<E, C> listener);
	void unregister(ControllerEventListener<E, C> listener);
}
