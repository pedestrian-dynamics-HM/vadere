package org.vadere.simulator.util;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.util.AbstractScenarioCheckerMessageFormatter;
import org.vadere.simulator.util.ScenarioCheckerMessage;

public class ConsoleScenarioCheckerMessageFormatter extends AbstractScenarioCheckerMessageFormatter {

	private final Scenario scenario;

	public ConsoleScenarioCheckerMessageFormatter(final Scenario scenario){
		this.scenario = scenario;
	}

	@Override
	protected void writeHeader(ScenarioCheckerMessage msg) {
//		sb.append(Messages.getString(msg.getMsgType().getLocalTypeId())))
	}

	@Override
	protected void writeMsg(ScenarioCheckerMessage msg) {

	}
}
