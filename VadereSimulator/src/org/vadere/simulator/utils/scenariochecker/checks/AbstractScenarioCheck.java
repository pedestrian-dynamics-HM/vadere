package org.vadere.simulator.utils.scenariochecker.checks;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessageBuilder;

public abstract class AbstractScenarioCheck implements ScenarioCheckerTest{

	protected ScenarioCheckerMessageBuilder msgBuilder;

	public AbstractScenarioCheck(){
		msgBuilder = new ScenarioCheckerMessageBuilder();
	}

}
