package org.vadere.gui.components.control;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.view.ScenarioNamePanel;
import org.vadere.gui.projectview.view.ScenarioPanel;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.util.ScenarioChecker;
import org.vadere.simulator.util.ScenarioCheckerMessage;
import org.vadere.state.scenario.ScenarioElement;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.PriorityQueue;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

public class ActionScenarioChecker extends AbstractAction implements Observer {

	private PriorityQueue<ScenarioCheckerMessage> errorMsg;
	private PriorityQueue<ScenarioCheckerMessage> warnMsg;
	private MsgDocument msgDocument;

	private IDrawPanelModel model;
	private ScenarioNamePanel view;

	public
	ActionScenarioChecker(String name, ScenarioNamePanel view) {
		super(name);
		this.errorMsg = new PriorityQueue<>();
		this.warnMsg = new PriorityQueue<>();
		this.view = view;
	}

	/**
	 * Handel click on traffic light icon and show all ScenarioChecker messages generated for the
	 * current state of the topography.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		VDialogManager.showMessageDialogWithBodyAndTextEditorPane(
				"Topography Checker",
				"The following problems where found",
				msgDocument,
				JOptionPane.INFORMATION_MESSAGE
		);
	}

	public void observerModel(IDrawPanelModel model){
		model.addObserver(this);
		this.model = model;
	}


	/**
	 * After each change of the Topography which yields a valid json representation check run the
	 * {@link ScenarioChecker} and change the icon respectively. This function also creates the
	 * message document presented in various dialog windows.
	 */
	@Override
	public void update(Observable o, Object arg) {
		if(model != null){
			check(model.getScenario());
		}
	}

	public void check(final Scenario scenario){
		ScenarioChecker checker = new ScenarioChecker(scenario);
		errorMsg.clear();
		warnMsg.clear();
		ScenarioPanel.setActiveTopographyErrorMsg(null);
		addMsg(checker.checkBuildingStep());

		view.setGreen();

		if (warnMsg.size() > 0) {
			view.setYellow();
		}

		if (errorMsg.size() > 0) {
			view.setRed();
		}
	}

	/**
	 * @return MsgDocument containing erros and warnings with links to the specific scenario element
	 */
	private MsgDocument checkerMessagesToDoc() {
		StringBuilder sb = new StringBuilder();
		MsgDocument doc = new MsgDocument();
		if (errorMsg.size() > 0) {
			sb.append("<h3> Errors </h3>").append("<br>");
			errorMsg.forEach(m -> msgToDocString(sb, m, doc));
		}

		if (warnMsg.size() > 0) {
			if (sb.length() > 0) {
				sb.append("<br>");
			}
			sb.append("<h3> Warnings </h3>").append("<br>");
			warnMsg.forEach(m -> msgToDocString(sb, m, doc));
		}

		if (errorMsg.size() == 0 && warnMsg.size() == 0) {
			sb.append("No Problems found.");
		}
		doc.setText(sb.toString());
		return doc;
	}


	private void msgToDocString(StringBuilder sb, ScenarioCheckerMessage msg, MsgDocument doc) {

		if (msg.hasTarget()){
			sb.append("[");
			msg.getMsgTarget().getTargets().forEach(t -> {
				doc.makeLink(t, sb);
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
		doc.setText(sb.toString());
	}

	private void addMsg(PriorityQueue<ScenarioCheckerMessage> msg) {
		msg.forEach(m -> {
			if (m.getMsgType().getId() >= 500) {
				warnMsg.add(m);
			} else {
				errorMsg.add(m);
			}
		});
		msgDocument = checkerMessagesToDoc();
		if (errorMsg.size() > 0) {
			ScenarioPanel.setActiveTopographyErrorMsg(msgDocument);
		}
	}


	/**
	 * Simple {@link JEditorPane} wrapper which manages the links within the document to highlight
	 * the {@link ScenarioElement} producing the topographyError / topographyWarning.
	 */
	class MsgDocument extends JEditorPane {
		HashMap<String, ScenarioElement> linkMap;
		int id;

		MsgDocument() {
			linkMap = new HashMap<>();
			id = 0;
			setContentType("text/html");
			setEditable(false);
			addHyperlinkListener(e -> {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					ScenarioElement element = linkMap.getOrDefault(e.getDescription(), null);
					if (element != null) {
						if (model != null){
							model.setSelectedElement(element);
						}
					}
				}
			});
		}

		void makeLink(ScenarioElement element, StringBuilder sb) {
			linkMap.put("element/id/" + id, element);
			sb.append("<a href='element/id/")
					.append(id).append("'>")
					.append(element.getClass().getSimpleName())
					.append("{Id:").append(element.getId()).append("}")
					.append("</a>");
			id++;
		}

	}
}
