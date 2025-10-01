package org.vadere.simulator.utils.scenariochecker;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.SimulatorLocalization;

public class ConsoleScenarioCheckerMessageFormatter extends AbstractScenarioCheckerMessageFormatter {

	private final Scenario scenario;

	public ConsoleScenarioCheckerMessageFormatter(final Scenario scenario){
		this.scenario = scenario;
	}

	@Override
	protected void writeHeader(ScenarioCheckerMessage msg) {
		sb.append(SimulatorLocalization.getString(currentType.getLocalTypeId())).append(":\n");
	}

	@Override
	protected void writeMsg(ScenarioCheckerMessage msg) {
		sb.append("   ")
				.append(SimulatorLocalization.getString(msg.getReason().getLocalMessageId()))
				.append(" ")
				.append(msg.getReasonModifier())
				.append("\n");
	}
}
