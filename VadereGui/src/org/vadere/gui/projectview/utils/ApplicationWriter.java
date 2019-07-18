package org.vadere.gui.projectview.utils;


import org.vadere.gui.projectview.control.ActionLoadProject;
import org.vadere.simulator.projects.ProjectWriter;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.IOException;

public class ApplicationWriter {

	private static Logger logger = Logger.getLogger(ApplicationWriter.class);

	public static void saveProject(final String filePath, final VadereProject project, final boolean override)
			throws IOException {
		String dirPath = ProjectWriter.getProjectDir(filePath);
		String fullpath = dirPath + "/" + IOUtils.VADERE_PROJECT_FILENAME;
		ProjectWriter.writeProjectFileJson(dirPath, project, override, false);
		logger.info(String.format("saved project '%s' at '%s'.", project.getName(), filePath));
		ActionLoadProject.addToRecentProjects(fullpath);
	}

}
