package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.projectview.view.JsonValidIndicator;
import org.vadere.gui.projectview.view.ScenarioPanel;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.util.TopographyChecker;
import org.vadere.simulator.util.TopographyCheckerMessage;
import org.vadere.simulator.util.TopographyCheckerMessageType;
import org.vadere.state.scenario.ScenarioElement;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.PriorityQueue;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

public class ActionTopographyCheckerMenu extends TopographyAction implements Observer {

	private final static ImageIcon iconRed = new ImageIcon(Resources.class.getResource("/icons/light_red_icon.png"));
	private final static ImageIcon iconYellow = new ImageIcon(Resources.class.getResource("/icons/light_yellow_icon.png"));
	private final static ImageIcon iconGreen = new ImageIcon(Resources.class.getResource("/icons/light_green_icon.png"));

	private final JsonValidIndicator jsonValidIndicator;
	private PriorityQueue<TopographyCheckerMessage> errorMsg;
	private PriorityQueue<TopographyCheckerMessage> warnMsg;
	private MsgDocument msgDocument;

	public ActionTopographyCheckerMenu(String name, IDrawPanelModel<?> panelModel, JsonValidIndicator jsonValidIndicator) {
		super(name, iconYellow, panelModel);
		this.jsonValidIndicator = jsonValidIndicator;
		this.errorMsg = new PriorityQueue<>();
		this.warnMsg = new PriorityQueue<>();
		panelModel.addObserver(this);
	}

	/**
	 * Handel click on traffic light icon and show all TopographyChecker messages generated for the
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

	/**
	 * After each change of the Topography which yields a valid json representation check run the
	 * {@link TopographyChecker} and change the icon respectively. This function also creates the
	 * message document presented in various dialog windows.
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (jsonValidIndicator.isValid()) {
			TopographyChecker checker = new TopographyChecker(getScenarioPanelModel().getTopography());
			errorMsg.clear();
			warnMsg.clear();
			ScenarioPanel.setActiveTopographyErrorMsg(null);
			addMsg(checker.checkBuildingStep());

			putValue(Action.SMALL_ICON, iconGreen);

			if (warnMsg.size() > 0) {
				putValue(Action.SMALL_ICON, iconYellow);
			}

			if (errorMsg.size() > 0) {
				putValue(Action.SMALL_ICON, iconRed);
			}
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


	private void msgToDocString(StringBuilder sb, TopographyCheckerMessage msg, MsgDocument doc) {
		sb.append(Messages.getString(msg.getMsgType().getLocalTypeId())).append(":  ");

		sb.append("[");
		msg.getMsgTarget().getTargets().forEach(t -> {
			doc.makeLink(t, sb);
			sb.append(", ");
		});
		sb.setLength(sb.length()-2);
		sb.append("] ");

		sb.append("Reason: ").append(Messages.getString(msg.getReason().getLocalMessageId()));
		if (!msg.getReasonModifier().isEmpty()) {
			sb.append(" ").append(msg.getReasonModifier());
		}
		sb.append("<br>");
		doc.setText(sb.toString());
	}

	private void addMsg(List<TopographyCheckerMessage> msg) {
		msg.forEach(m -> {
			if (m.getMsgType().equals(TopographyCheckerMessageType.WARN)) {
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
	 * the {@link ScenarioElement} producing the error / warning.
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
						getScenarioPanelModel().setSelectedElement(element);
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
