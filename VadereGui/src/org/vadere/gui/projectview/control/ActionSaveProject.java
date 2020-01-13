package org.vadere.gui.projectview.control;


import org.vadere.gui.components.control.ActionScenarioChecker;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.IOException;

public class ActionSaveProject extends ActionAbstractSaveProject {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ActionSaveProject.class);

	public ActionSaveProject(final String name, final ProjectViewModel model) {
		super(name, model);
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		if (!model.isProjectAvailable()) {
			return;
		}
		try {
			if (!VadereConfig.getConfig().getBoolean("Project.ScenarioChecker.active")) {
				// run ScenarioChecker at least once before saving changes.
				ActionScenarioChecker.performManualCheck(model.getCurrentScenario());
			}
			if (VDialogManager.continueSavingDespitePossibleJsonError())
				saveProjectUnlessUserCancels(model);
		} catch (IOException e) {
		}
	}
}
