package org.vadere.simulator.utils.scenariochecker.checks;

import java.util.PriorityQueue;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;

public interface ScenarioCheckerTest {
  PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(final Scenario scenario);
}
