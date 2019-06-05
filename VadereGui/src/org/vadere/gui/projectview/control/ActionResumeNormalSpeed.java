package org.vadere.gui.projectview.control;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.VadereScenarioTableModel;
import org.vadere.gui.projectview.model.VadereState;
import org.vadere.simulator.projects.Scenario;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionResumeNormalSpeed extends AbstractAction {


	private ProjectViewModel model;

	public ActionResumeNormalSpeed(String name, ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Scenario scenario = model.getRunningScenario().getScenario();
		model.getProject().resumePausedScenarios();
		model.getScenarioTableModel().replace(scenario,
				new VadereScenarioTableModel.VadereDisplay(scenario, VadereState.RUNNING));
	}
}
