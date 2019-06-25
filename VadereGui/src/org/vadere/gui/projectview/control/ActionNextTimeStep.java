package org.vadere.gui.projectview.control;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.VadereScenarioTableModel;
import org.vadere.gui.projectview.model.VadereState;
import org.vadere.simulator.projects.Scenario;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionNextTimeStep extends AbstractAction {

	private ProjectViewModel model;

	public ActionNextTimeStep(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		Scenario scenario = model.getRunningScenario().getScenario();
		model.getScenarioTableModel().replace(scenario,
				new VadereScenarioTableModel.VadereDisplay(scenario, VadereState.STEP));
		if(model.getProject().isScenarioInSingleStepMode()){
			model.getProject().nextSimCommand(-1);
		} else {
			// activate SingleStepMode and start waiting
			model.getProject().setSingleStepMode(true);
			model.getProject().nextSimCommand(-1); // wait on each simulation step.
		}
	}
}
