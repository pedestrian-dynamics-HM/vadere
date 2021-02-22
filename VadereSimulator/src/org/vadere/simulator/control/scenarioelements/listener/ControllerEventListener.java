package org.vadere.simulator.control.scenarioelements.listener;

import org.vadere.simulator.control.scenarioelements.ScenarioElementController;

/**
 * notify ScenarioElementController about an event of the given Controller.
 * @param <E>	Scenario Element Type just worked on
 * @param <C>	Controller which called the listener
 */
public interface ControllerEventListener <E, C extends ScenarioElementController> {

	 E notify(C controller, double simTimeInSec, E scenarioElement);
}
