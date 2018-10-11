package org.vadere.simulator.util.checks;

import org.vadere.simulator.util.ScenarioCheckerMessageBuilder;

public abstract class AbstractScenarioCheck implements ScenarioCheckerTest{

	protected ScenarioCheckerMessageBuilder msgBuilder;

	public AbstractScenarioCheck(){
		msgBuilder = new ScenarioCheckerMessageBuilder();
	}

}
