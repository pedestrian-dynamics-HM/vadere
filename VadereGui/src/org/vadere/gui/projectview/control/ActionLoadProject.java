package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.VadereApplication;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.simulator.projects.migration.MigrationAssistant;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class ActionLoadProject extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private static Logger logger = LogManager.getLogger(ActionLoadProject.class);

	private ProjectViewModel model;

	public ActionLoadProject(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		loadProject(false);
	}

	public void loadProject(boolean isRemigrationLoading) {
		try {
			// 1. ask for save project if it has changed
			if (!ActionAbstractSaveProject.askSaveUnlessUserCancels(model)) {
				return;
			}

			// 2. get the path from the user
			String projectFilePath = VDialogManager.loadProjectDialog();

			if (projectFilePath != null) {

				Object[] options = new Object[Version.values().length+1];
				System.arraycopy(Version.values(),0,options, 0, Version.values().length);
				options[options.length-1] = Messages.getString("ProjectView.chooseMigrationBaseDialog.defaultOption");

				//TODO: [refactoring]: static call which has side-effect to the following call!
				if (isRemigrationLoading) {
					Object option = JOptionPane.showInputDialog(null,
							Messages.getString("ProjectView.chooseMigrationBaseDialog.text"),
							Messages.getString("ProjectView.chooseMigrationBaseDialog.title"),
							JOptionPane.INFORMATION_MESSAGE, null,
							options, options[options.length-1]);

					if(option.equals(options[options.length-1])) {
						MigrationAssistant.setReapplyLatestMigrationFlag();
					}
					else {
						Version version = (Version)option;
						MigrationAssistant.setReapplyLatestMigrationFlag(version);
					}

				}

				// 3. load project
				loadProjectByPath(model, projectFilePath);

			} else {
				logger.info(String.format("user canceled load project."));
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static void addToRecentProjects(String path) {
		String listStr = Preferences.userNodeForPackage(VadereApplication.class).get("recent_projects", "");
		String str = path; // make sure the new one is at top position
		if (listStr.length() > 0) {
			String[] list = listStr.split(",");
			for (int i = 0; i < list.length; i++) {
				String entry = list[i];
				if (i < 10 && !entry.equals(path) && Files.exists(Paths.get(entry)))
					str += "," + entry;
			}
		}
		Preferences.userNodeForPackage(VadereApplication.class).put("last_used_project", path);
		Preferences.userNodeForPackage(VadereApplication.class).put("recent_projects", str);
		ProjectView.getMainWindow().updateRecentProjectsMenu();
	}

	public static void loadProjectByPath(ProjectViewModel projectViewModel, String projectFilePath) {
		try {
			VadereProject project = IOVadere. readProjectJson(projectFilePath);
			projectViewModel.setCurrentProjectPath(projectFilePath);
			projectViewModel.setProject(project);

			projectViewModel.refreshOutputTable();

			// select and load first scenario from list
			projectViewModel.setSelectedRowIndexInScenarioTable(0);

			// change the default directory for searching files
			Preferences.userNodeForPackage(VadereApplication.class).put("default_directory",
					projectViewModel.getCurrentProjectPath());

			addToRecentProjects(projectFilePath);
			ProjectView.getMainWindow().setProjectSpecificActionsEnabled(true);

			logger.info(String.format("project '%s' loaded.", projectViewModel.getProject().getName()));

			// results from migration assistant if he was active
			int[] stats = project.getMigrationStats();

			if (stats[1] > 0 || stats[2] > 0) { // scenarios: [0] total, [1] legacy'ed, [2] unmigratable
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					@Override
					public Void doInBackground() {
						int total = stats[0];
						int migrated = stats[1];
						int nonmigratable = stats[2];
						int untouched = total - migrated - nonmigratable;

						// TODO pull this text from the language files

						String message =
								"The migration assistant analyzed the " + total + " scenarios in the scenarios and output " +
										"directories of this project and attempted to upgrade them to the latest version "
										+ Version.latest().label() + ".\n" +
										"Log-files have been created in legacy/scenarios and legacy/output.\n\n";

						if (untouched > 0)
							message += untouched + " of the scenarios were already up to date.\n\n";
						if (nonmigratable > 0)
							message += nonmigratable
									+ " scenarios could not automatically be upgraded and were moved to the legacy-folder. They can't be opened unless the upgrade is done manually.\n\n";
						if (migrated > 0)
							message += migrated
									+ " scenarios were successfully upgraded. The old versions were moved to the legacy-folder.\n\n";

						JOptionPane.showMessageDialog(
								ProjectView.getMainWindow(),
								message, "Migration assistant",
								JOptionPane.INFORMATION_MESSAGE);
						return null;
					}
				};
				worker.execute();
			}

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Migration assistant",
					JOptionPane.ERROR_MESSAGE);
			logger.error(e);
			e.printStackTrace();
		}
	}
}
