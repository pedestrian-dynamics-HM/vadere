package org.vadere.simulator.util.checks.simulation;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.util.ScenarioCheckerMessage;
import org.vadere.simulator.util.checks.AbstractScenarioCheck;
import org.vadere.state.attributes.AttributesSimulation;

import java.util.PriorityQueue;

public class SimulationTimeStepLengthCheck extends AbstractScenarioCheck {
	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Scenario scenario) {
		AttributesSimulation simAttr = scenario.getAttributesSimulation();
		//todo....
		return null;
	}
}
