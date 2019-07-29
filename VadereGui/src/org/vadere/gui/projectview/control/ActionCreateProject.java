package org.vadere.gui.projectview.control;


import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.*;

import static org.vadere.gui.projectview.control.ActionAbstractSaveProject.saveProjectUnlessUserCancels;

public class ActionCreateProject extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ActionCreateProject.class);

	private ProjectViewModel model;

	public ActionCreateProject(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		try {
			if (!ActionAbstractSaveProject.askSaveUnlessUserCancels(model))
				return;

			// get the name of the new project
			String newProjectName =
					JOptionPane.showInputDialog(null, Messages.getString("NewProjectName.text"),
							"InfoBox: " + Messages.getString("NewProjectName.title"),
							JOptionPane.PLAIN_MESSAGE);

			if (newProjectName == null || newProjectName.trim().length() == 0) {
				logger.info("invalid project name");
			} else {

				final String projectDirectory= VDialogManager.saveProjectDialog();

				if(projectDirectory == null)
					return; // do not create project without saving it first.

				VadereProject project = new VadereProject(newProjectName, new ArrayList<>(), Paths.get(projectDirectory));
				model.setProject(project);
				model.refreshOutputTable();
				saveProjectUnlessUserCancels(model);
				ProjectView.getMainWindow().setProjectSpecificActionsEnabled(true);
				logger.info("create project: " + newProjectName);
			}
		} catch (IOException e) {
		}
	}
}
