package org.vadere.gui.projectview.control;


import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Ask the user to select a scenario-file from an output directory to convert this file to a
 * {@link org.vadere.simulator.projects.Scenario} scenario.
 *
 */
public class ActionGenerateScenarioFromOutputFile extends ActionAbstractAddScenario {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ActionGenerateScenarioFromOutputFile.class);

	private ProjectViewModel model;

	public ActionGenerateScenarioFromOutputFile(final String name, final ProjectViewModel model) {
		super(name, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		addScenario();
	}

	@Override
	protected Scenario generateVadere(final String name) throws IOException {
		try {
			FileFilter filter = new FileNameExtensionFilter("Scenario", IOUtils.SCENARIO_FILE_EXTENSION);
			String path =
					IOUtils.chooseFile(Messages.getString("LoadOutputText"), model.getCurrentProjectPath(), filter);
			if (path == null) {
				return null;
			}
			Scenario vadere = IOVadere.fromJson(IOUtils.readTextFile(path));
			vadere.setName(name);
			return vadere;
		} catch (IOException e) {
			logger.error(String.format("could not read scenario file: '%s'", e.getLocalizedMessage()));
			throw (e);
		}
	}
}
