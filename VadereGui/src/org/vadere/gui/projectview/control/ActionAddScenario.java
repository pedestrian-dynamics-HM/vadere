package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.state.scenario.Topography;

import java.io.IOException;

/**
 * Add a new and fresh {@link org.vadere.simulator.projects.ScenarioRunManager} scenario to the
 * {@link org.vadere.simulator.projects.VadereProject} project.
 *
 */
public class ActionAddScenario extends ActionAbstractAddScenario {

	private static final long serialVersionUID = 1L;
	private static Logger logger = LogManager.getLogger(ActionAddScenario.class);

	private Topography topography;

	public ActionAddScenario(final String name, final ProjectViewModel model) {
		super(name, model);
		this.topography = null;
	}

	@Override
	protected ScenarioRunManager generateVadere(final String name) throws IOException {
		ScenarioRunManager newScenario = new ScenarioRunManager(name);
		if (topography != null) {
			newScenario.setTopography(topography);
			logger.info("set topography to scenario");
		}
		return newScenario;
	}

	public void setTopography(final Topography topography) {
		this.topography = topography;
	}
}
