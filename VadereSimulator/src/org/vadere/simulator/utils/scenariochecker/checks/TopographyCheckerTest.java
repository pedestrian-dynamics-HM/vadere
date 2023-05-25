package org.vadere.simulator.utils.scenariochecker.checks;

import java.util.PriorityQueue;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.state.scenario.Topography;

public interface TopographyCheckerTest extends ScenarioCheckerTest {

  @Override
  default PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(final Scenario scenario) {
    return runScenarioCheckerTest(scenario.getTopography());
  }

  PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(final Topography topography);
}
