package org.vadere.gui.components.control;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.view.ScenarioNamePanel;
import org.vadere.gui.projectview.view.ScenarioPanel;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioChecker;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.util.config.VadereConfig;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.PriorityQueue;

import javax.swing.*;

public class ActionScenarioChecker extends AbstractAction implements Observer {

	private PriorityQueue<ScenarioCheckerMessage> messages;
	private ScenarioCheckerMessageDocumentView msgDocument;

	private IDrawPanelModel model;
	private ScenarioNamePanel view;

	public ActionScenarioChecker(String name, ScenarioNamePanel view) {
		super(name);
		this.messages = new PriorityQueue<>();
		this.view = view;
	}

	/**
	 * Handel click on traffic light icon and show all ScenarioChecker messages generated for the
	 * current state of the topography.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (!VadereConfig.getConfig().getBoolean("Project.ScenarioChecker.active")) {
			VDialogManager.showMessageDialogWithTextArea(
					Messages.getString("ProjectView.ScenarioChecker.title"),
					Messages.getString("ProjectView.ScenarioChecker.deactive.text"),
					JOptionPane.INFORMATION_MESSAGE
			);
		} else {
			VDialogManager.showMessageDialogWithBodyAndTextEditorPane(
					Messages.getString("ProjectView.ScenarioChecker.title"),
					Messages.getString("ProjectView.ScenarioChecker.active.text"),
					msgDocument,
					JOptionPane.INFORMATION_MESSAGE
			);
		}


	}

	public void observerModel(IDrawPanelModel model) {
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
		if (model != null) {
			check(model.getScenario());
		}
	}


	//		VadereConfig.getConfig().setProperty("Project.ScenarioChecker.active", item.getState());

	public void check(final Scenario scenario) {
		if (!VadereConfig.getConfig().getBoolean("Project.ScenarioChecker.active")) {
			// ScenarioChecker is deactivated.
			view.setDeactivate();
			return;
		}
		run_check(scenario);

		view.setGreen();

		if (!messages.isEmpty()) {

			if (messages.peek().getMsgType().isWarnMsg()) {
				view.setYellow();
			}

			if (messages.peek().getMsgType().isErrorMsg()) {
				view.setRed();
				ScenarioPanel.setActiveTopographyErrorMsg(msgDocument);
			}
		}
	}

	private void run_check(final Scenario scenario) {
		ScenarioChecker checker = new ScenarioChecker(scenario);
		messages.clear();
		ScenarioPanel.setActiveTopographyErrorMsg(null);
		messages = checker.checkBuildingStep();
		msgDocument = new ScenarioCheckerMessageDocumentView(model);
		msgDocument.setMessages(messages);
	}

	public static void performManualCheck(final Scenario scenario) {
		ActionScenarioChecker action = new ActionScenarioChecker("", null);
		action.run_check(scenario);
		if (!action.messages.isEmpty() && action.messages.peek().getMsgType().isErrorMsg()) {
			ScenarioPanel.setActiveTopographyErrorMsg(action.msgDocument);
		} else {
			ScenarioPanel.setActiveTopographyErrorMsg(null);
		}
	}
}
