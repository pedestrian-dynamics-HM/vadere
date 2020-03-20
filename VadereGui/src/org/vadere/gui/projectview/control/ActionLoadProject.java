package org.vadere.gui.projectview.control;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.simulator.projects.migration.MigrationOptions;
import org.vadere.simulator.projects.migration.MigrationResult;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ActionLoadProject extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ActionLoadProject.class);

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
					MigrationOptions migrationOptions;
					Object option = JOptionPane.showInputDialog(null,
							Messages.getString("ProjectView.chooseMigrationBaseDialog.text"),
							Messages.getString("ProjectView.chooseMigrationBaseDialog.title"),
							JOptionPane.INFORMATION_MESSAGE, null,
							options, options[options.length-1]);

					if(option.equals(options[options.length-1])) {
						migrationOptions = MigrationOptions.reapplyWithAutomaticVersionDiscorvery();
					}
					else {
						migrationOptions = MigrationOptions.reapplyFromVersion((Version)option);
					}
					// 3. loadFromFilesystem project
					loadProjectByPath(model, projectFilePath, migrationOptions);

				} else {
					// 3. loadFromFilesystem project
					loadProjectByPath(model, projectFilePath);
				}


			} else {
				logger.info("User canceled loadFromFilesystem project.");
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static void addToRecentProjects(String path) {
		List<String> existingStoredPaths = VadereConfig.getConfig().getList(String.class,"History.recentProjects", Collections.EMPTY_LIST);
		existingStoredPaths.add(0, path);
		existingStoredPaths = existingStoredPaths.stream()
				.filter(entry -> Files.exists(Paths.get(entry)))
				.distinct()
				.limit(10)
				.collect(Collectors.toList());
		VadereConfig.getConfig().setProperty("History.lastUsedProject", path);
		VadereConfig.getConfig().setProperty("History.recentProjects", existingStoredPaths);
		ProjectView.getMainWindow().updateRecentProjectsMenu();
	}

	public static void loadProjectByPath(ProjectViewModel projectViewModel, String projectFilePath){
		loadProjectByPath(projectViewModel, projectFilePath, MigrationOptions.defaultOptions());
	}

	public static void loadProjectByPath(ProjectViewModel projectViewModel, String projectFilePath, MigrationOptions options) {
		try {
			VadereProject project = IOVadere.readProjectJson(projectFilePath, options);
			projectViewModel.setProject(project);

			projectViewModel.refreshOutputTable();
			logger.info("refreshed output table - 2");

			// select and loadFromFilesystem first scenario from list
			projectViewModel.setSelectedRowIndexInScenarioTable(0);
            logger.info("selected the first scenario");

			// change the default directory for searching files
			VadereConfig.getConfig().setProperty("ProjectView.defaultDirectory",
					projectViewModel.getCurrentProjectPath());
			addToRecentProjects(projectFilePath);
			ProjectView.getMainWindow().setProjectSpecificActionsEnabled(true);
			logger.info(String.format("project '%s' loaded.", projectViewModel.getProject().getName()));

			// results from migration assistant if he was active
			MigrationResult stats = project.getMigrationStats();

			if (stats.legacy > 0 || stats.notmigratable > 0) { // scenarios: [0] total, [1] legacy'ed, [2] unmigratable
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					@Override
					public Void doInBackground() {
						String migrationResult = String.format("%s %s:\n\n",
								Messages.getString("MigrationAssistant.Results.title"),
								Version.latest().label());
						migrationResult += String.join("\n", getMigrationResult(stats));

						if (stats.legacy > 0) {
							migrationResult += String.format("\n\n%s", Messages.getString("MigrationAssistant.Results.migratedInfo"));
						}

						JOptionPane.showMessageDialog(
								ProjectView.getMainWindow(),
								migrationResult, Messages.getString("MigrationAssistant.title"),
								JOptionPane.INFORMATION_MESSAGE);

						return null;
					}
				};
				worker.execute();
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), Messages.getString("MigrationAssistant.title"),
					JOptionPane.ERROR_MESSAGE);
			logger.error("could not loadFromFilesystem project: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * The "MigrationResult" class cannot access "Messages.getString(...)"
	 * because of avoiding cyclic dependencies between view and controller classes.
	 * Therefore, translate the migration results here.
	 */
	public static List<String> getMigrationResult(MigrationResult migrationResult) {
		List<String> resultArray = new ArrayList<>();

		String resultLineTemplate = "%s: %d";
		resultArray.add(String.format(resultLineTemplate, Messages.getString("MigrationAssistant.Results.analyzed"), migrationResult.total));
		resultArray.add(String.format(resultLineTemplate, Messages.getString("MigrationAssistant.Results.migrated"), migrationResult.legacy));
		resultArray.add(String.format(resultLineTemplate, Messages.getString("MigrationAssistant.Results.upToDate"), migrationResult.upToDate));
		resultArray.add(String.format(resultLineTemplate, Messages.getString("MigrationAssistant.Results.notMigratable"), migrationResult.notmigratable));

		return resultArray;
	}
}
