package org.vadere.gui.projectview.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.projectview.VadereApplication;
import org.vadere.gui.projectview.control.ActionLoadProject;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.dataprocessing.ProjectWriter;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ApplicationWriter {

	private static Logger logger = LogManager.getLogger(ApplicationWriter.class);

	public static void saveProject(final String filePath, final VadereProject project, final boolean override)
			throws IOException {
		String dirPath = ProjectWriter.getProjectDir(filePath);
		String fullpath = dirPath + "/" + IOUtils.VADERE_PROJECT_FILENAME;
		ProjectWriter.writeProjectFileJson(dirPath, project, override, false);
		logger.info(String.format("saved project '%s' at '%s'.", project.getName(), filePath));
		ActionLoadProject.addToRecentProjects(fullpath);
	}

	public static void savePreferences() throws IOException, BackingStoreException {
		logger.info(String.format("saving preferences..."));
		Resources.getInstance("postvisualization").save(); // TODO [priority=medium] [task=refactoring] is this necessary? these file seem to have gotten changed last in 2014...
		Resources.getInstance("topologycreator").save();
		IOUtils.saveUserPreferences(VadereApplication.preferencesFilename,
				Preferences.userNodeForPackage(VadereApplication.class));
		logger.info(String.format("saved preferences."));
	}
}
