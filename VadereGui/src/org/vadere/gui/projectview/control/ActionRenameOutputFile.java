package org.vadere.gui.projectview.control;


import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.ProjectViewModel.OutputBundle;
import org.vadere.simulator.projects.io.IOOutput;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.*;

public class ActionRenameOutputFile extends AbstractAction {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(ActionRenameOutputFile.class);

	private ProjectViewModel model;

	public ActionRenameOutputFile(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		try {
			OutputBundle outputBundle = model.getSelectedOutputBundle();

			File directory = outputBundle.getDirectory();

			String newDirectoryName = JOptionPane.showInputDialog((Component) event.getSource(),
					Messages.getString("listMenuRenameOutputButtonAction.title"),
					directory.getName());

			String directoryName = directory.getName();

			if (newDirectoryName != null && !newDirectoryName.trim().isEmpty()) {
				newDirectoryName = newDirectoryName.trim();
			}

			if (newDirectoryName != null && !newDirectoryName.equals(directoryName)) {
				if (IOOutput.renameOutputDirectory(directory, newDirectoryName)) {
					model.refreshOutputTable();
					logger.info("rename output file " + directoryName + " => " + newDirectoryName);
				} else {
					logger.info("wrong file name for an output file: " + newDirectoryName);
					IOUtils.errorBox(Messages.getString("RenameFileErrorMessage.text"),
							Messages.getString("RenameFileErrorMessage.title"));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
