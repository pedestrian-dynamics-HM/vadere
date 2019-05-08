package org.vadere.gui.projectview.control;


import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.IOException;

public class ActionSaveAsProject extends ActionAbstractSaveProject {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ActionSaveAsProject.class);

	public ActionSaveAsProject(final String name, final ProjectViewModel model) {
		super(name, model);
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		if (!model.isProjectAvailable())
			return;

		if (VDialogManager.continueSavingDespitePossibleJsonError())
			if (!ActionAbstractSaveProject.doesUserChooseToSave(model)) {
				logger.info("user canceled save project as.");
				return;
			}

		// at this point, the model has a project path
		try {
			ActionAbstractSaveProject.saveProject(model);
		} catch (IOException e) {
		}
	}
}
