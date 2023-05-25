package org.vadere.simulator.utils.scenariochecker.checks;

import java.util.PriorityQueue;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessageBuilder;

public abstract class AbstractScenarioCheck implements ScenarioCheckerTest {

  protected ScenarioCheckerMessageBuilder msgBuilder;
  protected PriorityQueue<ScenarioCheckerMessage> messages;

  public AbstractScenarioCheck() {
    msgBuilder = new ScenarioCheckerMessageBuilder();
    messages = new PriorityQueue<>();
  }
}
