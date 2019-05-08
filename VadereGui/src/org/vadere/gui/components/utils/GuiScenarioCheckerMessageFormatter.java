package org.vadere.gui.components.utils;

import org.vadere.gui.components.control.ScenarioCheckerMessageDocumentView;
import org.vadere.simulator.utils.scenariochecker.AbstractScenarioCheckerMessageFormatter;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessageType;

import java.util.Comparator;

public class GuiScenarioCheckerMessageFormatter extends AbstractScenarioCheckerMessageFormatter {

	final ScenarioCheckerMessageDocumentView view;
	boolean currentTypeChanged;
	String currTabString;
	boolean currTabStringChanged;

	public GuiScenarioCheckerMessageFormatter(ScenarioCheckerMessageDocumentView view) {
		this.view = view;
		currentTypeChanged = false;
		currTabStringChanged = false;
		currentType = null;
		currTabString = null;
	}

	/**
	 * Sort messages by tabs based on gui view
	 * @return
	 */
	@Override
	protected Comparator<ScenarioCheckerMessage> getComparator() {
		return ScenarioCheckerMessage::compareOrdinal;
	}

	@Override
	protected boolean isNewType(ScenarioCheckerMessage msg) {
		String tabString = getTabName(msg.getMsgType());
		if (currTabString == null || !currTabString.equals(tabString)){
			currTabString = tabString;
			currTabStringChanged = true;
		} else {
			currTabStringChanged = false;
		}

		if (currentType == null || !currentType.equals(msg.getMsgType())){
			currentType = msg.getMsgType();
			currentTypeChanged = true;
		} else {
			currentTypeChanged = false;
		}

		return currentTypeChanged || currTabStringChanged;
	}

	private String getTabName(ScenarioCheckerMessageType type){
		String ret="";
		switch (type){
			case TOPOGRAPHY_ERROR: case TOPOGRAPHY_WARN:
				ret = Messages.getString("Tab.Topography.title");
				break;
			case DATA_PROCESSOR_ERROR: case DATA_PROCESSOR_WARN:
				ret = Messages.getString("Tab.OutputProcessors.title");
				break;
			case MODEL_ATTR_ERROR: case MODEL_ATTR_WARN:
				ret = Messages.getString("Tab.Model.title");
				break;
			case SIMULATION_ATTR_ERROR: case SIMULATION_ATTR_WARN:
				ret = Messages.getString("Tab.Simulation.title");
				break;
		}
		return ret;
	}

	@Override
	protected void writeHeader(ScenarioCheckerMessage msg) {
		if (currTabStringChanged){
			if(sb.length() > 0){
				sb.append("<br>");
			}

			sb.append("<h3>")
					.append(currTabString)
					.append(" Tab")
					.append("</h3>");
		}

		if (currentTypeChanged){
			sb.append("<h4>")
					.append(Messages.getString(msg.getMsgType().getLocalTypeId()))
					.append("</h4>");
		}

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

		sb.append("<b>Reason:</b> ").append(Messages.getString(msg.getReason().getLocalMessageId()));
		if (!msg.getReasonModifier().isEmpty()) {
			sb.append(" ").append(msg.getReasonModifier());
		}
		sb.append("<br>");
	}
}
