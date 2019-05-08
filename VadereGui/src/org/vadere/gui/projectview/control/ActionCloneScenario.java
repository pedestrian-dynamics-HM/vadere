package org.vadere.gui.projectview.control;


import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.simulator.projects.ProjectWriter;
import org.vadere.simulator.projects.Scenario;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.*;

public class ActionCloneScenario extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ActionCloneScenario.class);
	private ProjectViewModel model;

	public ActionCloneScenario(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		ProjectViewModel.ScenarioBundle optionalScenarioBundle = model.getSelectedScenarioBundle();

		Scenario scenario = optionalScenarioBundle.getScenario();
		Scenario clonedScenario = scenario.clone();
		String name = scenario.getName() + "_" + Messages.getString("Clone.text") + "_";
		int index = 1;
		while (model.isScenarioNameInConflict(name + index)) {
			index++;
		}
		clonedScenario.setName(name + index);

		try {
			ProjectWriter.writeScenarioFileJson(model.getCurrentProjectPath(), clonedScenario);
			model.addScenario(clonedScenario);
			logger.info("clone scenario: " + scenario + " => " + clonedScenario);
			model.selectScenario(clonedScenario);
		} catch (IOException e) {
			logger.error("could not clone scenario: " + scenario + " => " + clonedScenario
					+ ", since " + e.getLocalizedMessage());
		}
	}
}
