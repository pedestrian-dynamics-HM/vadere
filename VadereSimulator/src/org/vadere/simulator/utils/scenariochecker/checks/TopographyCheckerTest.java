package org.vadere.simulator.utils.scenariochecker.checks;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.state.scenario.Topography;

import java.util.PriorityQueue;

public interface TopographyCheckerTest extends ScenarioCheckerTest {

	@Override
	default PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(final Scenario scenario){
		return runScenarioCheckerTest(scenario.getTopography());
	}

	PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(final Topography topography);


}
