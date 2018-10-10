package org.vadere.gui.components.utils;

import org.vadere.gui.components.control.ScenarioCheckerMessageDocumentView;
import org.vadere.simulator.util.AbstractScenarioCheckerMessageFormatter;
import org.vadere.simulator.util.ScenarioCheckerMessage;

public class GuiScenarioCheckerMessageFormatter extends AbstractScenarioCheckerMessageFormatter {

	final ScenarioCheckerMessageDocumentView view;

	public GuiScenarioCheckerMessageFormatter(ScenarioCheckerMessageDocumentView view) {
		this.view = view;
	}

	@Override
	protected void writeHeader(ScenarioCheckerMessage msg) {
		sb.append("<h4>")
			.append(Messages.getString(msg.getMsgType().getLocalTypeId()))
			.append("</h4>");

	}

	@Override
	protected void writeMsg(ScenarioCheckerMessage msg) {
		if (msg.hasTarget()){
			sb.append("[");
			msg.getMsgTarget().getTargets().forEach(element -> {
				int id = view.makeLink(element);

				sb.append("<a href='element/id/")
						.append(id).append("'>")
						.append(element.getClass().getSimpleName())
						.append("{Id:").append(element.getId()).append("}")
						.append("</a>");
				sb.append(", ");
			});
			sb.setLength(sb.length()-2);
			sb.append("] ");
		}

		sb.append("Reason: ").append(Messages.getString(msg.getReason().getLocalMessageId()));
		if (!msg.getReasonModifier().isEmpty()) {
			sb.append(" ").append(msg.getReasonModifier());
		}
		sb.append("<br>");
	}
}
