package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.ProjectWriter;
import org.vadere.util.io.IOUtils;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.io.IOException;

public abstract class ActionAbstractAddScenario extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private static Logger logger = LogManager.getLogger(ActionAbstractAddScenario.class);

	private ProjectViewModel model;

	protected ActionAbstractAddScenario(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		addScenario();
	}

	/**
	 * Try to add a scenario.
	 */
	protected void addScenario() {
		String scenarioName = getScenarioName();

		// user select abort
		if (scenarioName == null)
			return;

		if (isScenarioNameValid(scenarioName)) {
			try {
				ScenarioRunManager vadere = generateVadere(scenarioName);
				if (vadere == null) // e.g. canceled by user
					return;
				addScenario(vadere);
			} catch (IOException e) {
				logger.error(String.format("error during output load: '%s'", e.getLocalizedMessage()));
			}
		} else {
			IOUtils.errorBox(Messages.getString("renameErrorDialog.text"),
					Messages.getString("renameErrorDialog.title"));
		}
	}

	private boolean isScenarioNameValid(final String name) {
		return !model.isScenarioNameInConflict(name);
	}

	protected abstract ScenarioRunManager generateVadere(final String name) throws IOException;

	protected void addScenario(final ScenarioRunManager scenarioRM) throws IOException {
		ProjectWriter.writeScenarioFileJson(model.getCurrentProjectPath(), scenarioRM);
		model.addScenario(scenarioRM);
		logger.info("add scenario: " + scenarioRM);
		model.selectScenario(scenarioRM);
	}

	protected String getScenarioName() {
		return JOptionPane.showInputDialog(null,
				Messages.getString("ProjectView.addScenarioDialog.text"),
				Messages.getString("ProjectView.addScenarioDialog.defaultValue"));
	}
}
