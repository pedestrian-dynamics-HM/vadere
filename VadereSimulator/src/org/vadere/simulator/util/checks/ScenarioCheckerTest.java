package org.vadere.simulator.util.checks;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.util.ScenarioCheckerMessage;

import java.util.PriorityQueue;

public interface ScenarioCheckerTest {
	PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(final Scenario scenario);
}
