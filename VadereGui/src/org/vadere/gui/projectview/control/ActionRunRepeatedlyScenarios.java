package org.vadere.gui.projectview.control;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.VTable;
import org.vadere.simulator.projects.Scenario;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

public class ActionRunRepeatedlyScenarios extends AbstractAction {

	private final ProjectViewModel model;
	private final VTable scenarioTable;
	private final int repetitions;

	public ActionRunRepeatedlyScenarios(final String name, final ProjectViewModel model, final VTable scenarioTable, final int repetitions) {
		super(name);
		this.model = model;
		this.repetitions = repetitions;
		this.scenarioTable = scenarioTable;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (model.runScenarioIsOk()) {
			Collection<Scenario> scenarios = model.getScenarios(scenarioTable.getSelectedRows());
			// make sure only one scenario is chosen
			if(scenarios.size()>1){
				throw new IllegalArgumentException("Please select a single scenario for the stochastic evaluation");
			}
			Scenario singleScenario = scenarios.iterator().next();
			for(int i_rep = 0; i_rep < repetitions; i_rep++)
			scenarios.add(singleScenario);
			model.getProject().runScenarios(scenarios);
			model.getProject().getSimulationResults();
		}
	}
}
