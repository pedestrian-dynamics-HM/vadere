package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.VadereScenarioTableModel;
import org.vadere.gui.projectview.model.VadereState;
import org.vadere.simulator.projects.ScenarioRunManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ActionPauseScenario extends AbstractAction {

	private static Logger logger = LogManager.getLogger(ActionPauseScenario.class);

	private ProjectViewModel model;

	public ActionPauseScenario(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}


	@Override
	public void actionPerformed(final ActionEvent e) {
		ProjectViewModel.ScenarioBundle optionalScenarioBundle = model.getRunningScenario();
		ScenarioRunManager scenario = optionalScenarioBundle.getScenario();
		if (model.getProject().isScenarioPaused()) {
			model.getProject().resumePausedScenarios();
			model.getScenarioTableModel().replace(scenario,
					new VadereScenarioTableModel.VadereDisplay(scenario, VadereState.RUNNING));
			logger.info(String.format("all running scenarios resumed"));
		} else {
			model.getProject().pauseRunnningScenario();
			model.getScenarioTableModel().replace(scenario,
					new VadereScenarioTableModel.VadereDisplay(scenario, VadereState.PAUSED));
			logger.info(String.format("all running scenarios paused"));
		}
	}
}
