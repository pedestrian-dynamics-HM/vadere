package org.vadere.gui.projectview.control;


import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.*;


public class ActionLoadRecentProject extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ActionLoadProject.class);

	private ProjectViewModel model;
	private String path;

	public ActionLoadRecentProject(final String path, final ProjectViewModel model) {
		super(Paths.get(path).getParent().toString());
		this.path = path;
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		if (Files.exists(Paths.get(path))) {
			try {
				if (!ActionAbstractSaveProject.askSaveUnlessUserCancels(model))
					return;
				ActionLoadProject.loadProjectByPath(model, path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else { // in case it was deleted/moved while the GUI was open
			String msg = "The project " + Paths.get(path).getParent().toString() + " has been deleted or moved.";
			JOptionPane.showMessageDialog(null, msg, "Project not found", JOptionPane.ERROR_MESSAGE, null);
			logger.error(msg);
			ProjectView.getMainWindow().updateRecentProjectsMenu();
		}
	}

}
