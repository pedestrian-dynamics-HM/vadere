package org.vadere.gui.projectview.control;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.VadereScenarioTableModel;
import org.vadere.gui.projectview.model.VadereState;
import org.vadere.simulator.projects.Scenario;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionPauseScenario extends AbstractAction {

	private static Logger logger = Logger.getLogger(ActionPauseScenario.class);

	private ProjectViewModel model;

	public ActionPauseScenario(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		ProjectViewModel.ScenarioBundle optionalScenarioBundle = model.getRunningScenario();
		Scenario scenario = optionalScenarioBundle.getScenario();
		if (! model.getProject().isScenarioPaused()) {
			model.getProject().pauseRunnningScenario();
			model.getScenarioTableModel().replace(scenario,
					new VadereScenarioTableModel.VadereDisplay(scenario, VadereState.PAUSED));
			logger.info(String.format("all running scenarios paused"));
		}
	}
}
