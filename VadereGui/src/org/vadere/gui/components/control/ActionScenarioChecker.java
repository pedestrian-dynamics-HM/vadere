package org.vadere.gui.components.control;

import org.vadere.gui.projectview.view.ScenarioNamePanel;
import org.vadere.gui.projectview.view.ScenarioPanel;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioChecker;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;

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

	public
	ActionScenarioChecker(String name, ScenarioNamePanel view) {
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
		messages.clear();
		ScenarioPanel.setActiveTopographyErrorMsg(null);
		messages = checker.checkBuildingStep();
		msgDocument = new ScenarioCheckerMessageDocumentView(model);
		msgDocument.setMessages(messages);

		view.setGreen();

		if (messages.size() > 0){

			if(messages.peek().getMsgType().isWarnMsg()) {
				view.setYellow();
			}

			if (messages.peek().getMsgType().isErrorMsg()) {
				view.setRed();
				ScenarioPanel.setActiveTopographyErrorMsg(msgDocument);
			}
		}
	}
}
