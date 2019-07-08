package org.vadere.gui.projectview.control;


import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.VadereScenarioTableModel;
import org.vadere.gui.projectview.model.VadereState;
import org.vadere.simulator.projects.ProjectWriter;
import org.vadere.simulator.projects.Scenario;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.*;

public class ActionRenameScenario extends AbstractAction {

	private static Logger logger = Logger.getLogger(ActionRenameScenario.class);

	private ProjectViewModel model;

	public ActionRenameScenario(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {

		ProjectViewModel.ScenarioBundle optionalScenarioBundle = model.getSelectedScenarioBundle();

		Scenario scenario = optionalScenarioBundle.getScenario();
		String oldName = optionalScenarioBundle.getScenario().getName();
		String newName =
				JOptionPane.showInputDialog(null, Messages.getString("listMenuRenameButtonAction.title"), oldName);

		if (newName == null) // clicking the cancel button in InputDialog returns null
			return; // -> stop executing this method

		newName = newName.trim();

		if (newName.length() > 0 && !model.isScenarioNameInConflict(newName)) {
			try {
				ProjectWriter.renameScenario(scenario, model.getCurrentProjectPath(), newName);
				model.saveScenarioToDisk(scenario);

				// remove and insertVertex the element such that the table is still sorted by name
				model.getScenarioTableModel().replace(scenario, new VadereScenarioTableModel.VadereDisplay(scenario, VadereState.INITIALIZED));
				model.refreshScenarioNames();
				/*
				 * ScenarioRunManager newScenario = scenario.clone();
				 * ProjectWriter.renameScenario(newScenario, model.getCurrentProjectPath(),
				 * newName.trim());
				 * ProjectWriter.writeScenarioFileJson(model.getCurrentProjectPath(), newScenario);
				 * model.getProject().removeScenario(scenario);
				 * model.getProject().addScenario(newScenario);
				 * model.getScenarioTableModel().replace(scenario,
				 * new VadereScenarioTableModel.VadereDisplay(newScenario,
				 * VadereState.INITIALIZED));
				 * model.getScenarioTableModel().fireTableDataChanged();
				 */
				logger.info("rename scenario: " + oldName + " => " + newName);
			} catch (IOException e) {
				logger.error("IO operations failed regarding renaming scenario " + oldName + " to " + newName + ":\n"
						+ e.getLocalizedMessage());
			}
		} else {
			JOptionPane.showMessageDialog((Component) event.getSource(),
					Messages.getString("RenameFileErrorMessage.text"),
					Messages.getString("RenameFileErrorMessage.title"), JOptionPane.ERROR_MESSAGE);
		}
	}
}
