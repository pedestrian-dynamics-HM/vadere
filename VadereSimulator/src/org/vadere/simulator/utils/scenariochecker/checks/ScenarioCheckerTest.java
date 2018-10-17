package org.vadere.simulator.utils.scenariochecker.checks;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;

import java.util.PriorityQueue;

public interface ScenarioCheckerTest {
	PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(final Scenario scenario);
}
