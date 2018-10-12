package org.vadere.simulator.util.checks;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.util.ScenarioCheckerMessage;
import org.vadere.simulator.util.checks.ScenarioCheckerTest;
import org.vadere.state.scenario.Topography;

import java.util.PriorityQueue;

public interface TopographyCheckerTest extends ScenarioCheckerTest {

	@Override
	default PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(final Scenario scenario){
		return runScenarioCheckerTest(scenario.getTopography());
	}

	PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(final Topography topography);


}
