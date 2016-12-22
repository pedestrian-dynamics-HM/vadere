package org.vadere.gui.projectview.control;

import java.io.IOException;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.ProjectViewModel.OutputBundle;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.IOOutput;

/**
 * Converts a selected output directory to a
 * {@link org.vadere.simulator.projects.Scenario} scenario.
 * 
 */
public class ActionOutputToScenario extends ActionAbstractAddScenario {

	private static final long serialVersionUID = 1L;
	private ProjectViewModel model;

	public ActionOutputToScenario(final String name, final ProjectViewModel model) {
		super(name, model);
		this.model = model;
	}

	@Override
	protected Scenario generateVadere(final String name) throws IOException {
		OutputBundle bundle = model.getSelectedOutputBundle();
		Scenario scenarioRM = IOOutput.readScenario(bundle.getDirectory());
		scenarioRM.setName(name);
		return scenarioRM;
	}

}
