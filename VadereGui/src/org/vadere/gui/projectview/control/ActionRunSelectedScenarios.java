package org.vadere.gui.projectview.control;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.VTable;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionRunSelectedScenarios extends AbstractAction {

	private final ProjectViewModel model;
	private final VTable scenarioTable;

	public ActionRunSelectedScenarios(final String name, final ProjectViewModel model, final VTable scenarioTable) {
		super(name);
		this.model = model;
		this.scenarioTable = scenarioTable;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (model.runScenarioIsOk()) {
			model.getProject().runScenarios(model.getScenarios(scenarioTable.getSelectedRows()));
		}
	}
}
