package org.vadere.gui.projectview.control;


import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.io.IOException;

/**
 * Add a new and fresh {@link org.vadere.simulator.projects.Scenario} scenario to the
 * {@link org.vadere.simulator.projects.VadereProject} project.
 *
 */
public class ActionAddScenario extends ActionAbstractAddScenario {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ActionAddScenario.class);

	private Topography topography;

	public ActionAddScenario(final String name, final ProjectViewModel model) {
		super(name, model);
		this.topography = null;
	}

	@Override
	protected Scenario generateVadere(final String name) throws IOException {
		Scenario newScenario = new Scenario(name);
		newScenario.setDataProcessingJsonManager(DataProcessingJsonManager.createDefault());
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
