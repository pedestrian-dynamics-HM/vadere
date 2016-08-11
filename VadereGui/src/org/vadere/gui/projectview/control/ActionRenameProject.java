package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.util.io.IOUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ActionRenameProject extends AbstractAction {

	private static Logger logger = LogManager.getLogger(ActionRenameProject.class);

	private ProjectViewModel model;

	public ActionRenameProject(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		if (model.isProjectAvailable()) {
			String projectName = JOptionPane.showInputDialog(null, "New project name");
			String oldProjectName = model.getProject().getName();
			if (projectName != null && projectName.trim().length() > 0) {
				model.getProject().setName(projectName);
				model.fireProjectPropertyChanged();
				logger.info("rename project: " + oldProjectName + " => " + projectName);
			}
		} else {
			IOUtils.infoBox(Messages.getString("EmptyProjectErrorMessage.text"),
					Messages.getString("EmptyProjectErrorMessage.title"));
		}
	}
}
