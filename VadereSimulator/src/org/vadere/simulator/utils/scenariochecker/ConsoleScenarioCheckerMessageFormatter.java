package org.vadere.simulator.utils.scenariochecker;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.Messages;

public class ConsoleScenarioCheckerMessageFormatter extends AbstractScenarioCheckerMessageFormatter {

	private final Scenario scenario;

	public ConsoleScenarioCheckerMessageFormatter(final Scenario scenario){
		this.scenario = scenario;
	}

	@Override
	protected void writeHeader(ScenarioCheckerMessage msg) {
		sb.append(Messages.getString(currentType.getLocalTypeId())).append(":\n");
	}

	@Override
	protected void writeMsg(ScenarioCheckerMessage msg) {
		sb.append("   ")
				.append(Messages.getString(msg.getReason().getLocalMessageId()))
				.append(" ")
				.append(msg.getReasonModifier())
				.append("\n");
	}
}
