package org.vadere.gui.components.control;

import org.vadere.gui.components.utils.GuiScenarioCheckerMessageFormatter;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessageFormatter;
import org.vadere.state.scenario.ScenarioElement;

import java.util.HashMap;
import java.util.PriorityQueue;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

public class ScenarioCheckerMessageDocumentView extends JEditorPane {

	private final IDrawPanelModel model;
	private final ScenarioCheckerMessageFormatter formatter;
	HashMap<String, ScenarioElement> linkMap;
	int id;

	public ScenarioCheckerMessageDocumentView(IDrawPanelModel model) {
		this.model = model;
		this.formatter = new GuiScenarioCheckerMessageFormatter(this);
		linkMap = new HashMap<>();
		id = 0;
		setContentType("text/html");
		setEditable(false);
		addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				ScenarioElement element = linkMap.getOrDefault(e.getDescription(), null);
				if (element != null && this.model != null) {
					this.model.setSelectedElement(element);
				}
			}
		});
	}

	public void setMessages(PriorityQueue<ScenarioCheckerMessage> messages){
		setText(formatter.formatMessages(messages));
	}

	public int makeLink(ScenarioElement element) {
		int currId = id;
		id++;

		linkMap.put("element/id/" + currId, element);
		return currId;
	}
}
