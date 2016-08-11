package org.vadere.gui.projectview.control;

import javax.swing.*;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.ProjectViewModel.ScenarioBundle;

import java.awt.event.ActionEvent;

public class ActionRunSingleScenario extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private ProjectViewModel model;

	public ActionRunSingleScenario(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (model.runScenarioIsOk()) {
			ScenarioBundle sb = model.getSelectedScenarioBundle();
			model.getProject().runScenario(sb.getScenario());
		}
	}
}
