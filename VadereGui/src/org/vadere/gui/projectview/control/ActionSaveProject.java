package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.VDialogManager;

import java.awt.event.ActionEvent;
import java.io.IOException;

public class ActionSaveProject extends ActionAbstractSaveProject {

	private static final long serialVersionUID = 1L;
	private static Logger logger = LogManager.getLogger(ActionSaveProject.class);

	public ActionSaveProject(final String name, final ProjectViewModel model) {
		super(name, model);
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		if (!model.isProjectAvailable()) {
			return;
		}
		try {
			if (VDialogManager.continueSavingDespitePossibleJsonError())
				saveProjectUnlessUserCancels(model);
		} catch (IOException e) {
		}
	}
}
