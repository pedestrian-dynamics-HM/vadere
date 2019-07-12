package org.vadere.gui.postvisualization.control;


import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.view.DialogFactory;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.IOOutput;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class ActionOpenFile extends ActionVisualization {
	private static Logger logger = Logger.getLogger(ActionOpenFile.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private final PostvisualizationModel model;
	private String path = null;


	public ActionOpenFile(final String name, final Icon icon, final PostvisualizationModel model, final String path) {
		this(name, icon, model);
		this.path = path;
	}

	public ActionOpenFile(final String name, final Icon icon, final PostvisualizationModel model) {
		super(name, icon, model);
		this.model = model;
	}

	public ActionOpenFile(String name, final PostvisualizationModel model) {
		super(name, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {

		File file = null;
		if (path == null) {
			String path = VadereConfig.getConfig().getString("SettingsDialog.outputDirectory.path", "/");

			final JFileChooser fc = new JFileChooser(path);
			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();
			}
		} else {
			file = new File(path);
		}

		final File threadFile = file;

		if (file != null) {
			CONFIG.setProperty("SettingsDialog.outputDirectory.path", file.getParent());
			VadereConfig.getConfig().setProperty("SettingsDialog.outputDirectory.path", file.getParent());

			Runnable runnable = () -> {
				Player.getInstance(model).stop();
				final JFrame dialog = DialogFactory.createLoadingDialog();
				dialog.setVisible(true);
				try {
					Player.getInstance(model).stop();

					File scenarioOutputDir = threadFile.isDirectory() ? threadFile : threadFile.getParentFile();
					Optional<File> trajectoryFile =
							IOUtils.getFirstFile(scenarioOutputDir, IOUtils.TRAJECTORY_FILE_EXTENSION);
					Optional<File> snapshotFile =
							IOUtils.getFirstFile(scenarioOutputDir, IOUtils.SCENARIO_FILE_EXTENSION);

					if (trajectoryFile.isPresent() && snapshotFile.isPresent()) {
						Scenario vadere = IOOutput.readScenario(snapshotFile.get().toPath());
						model.init(IOOutput.readTrajectories(trajectoryFile.get().toPath(), vadere), vadere, trajectoryFile.get().getParent());
						model.notifyObservers();
						dialog.dispose();
						setLastDirectories(scenarioOutputDir);
					} else {
						throw new IOException(
								"could not find trajectory or snapshot file: " + trajectoryFile + ", " + snapshotFile);
					}

					// 1. cache file name in properties

				} catch (Exception e) {
					e.printStackTrace();
					logger.error(e.getMessage());
					JOptionPane.showMessageDialog(
							null,
							Messages.getString("InformationDialogFileError") + " - "
									+ e.getMessage(),
							Messages.getString("InformationDialogError.title"),
							JOptionPane.ERROR_MESSAGE);
				}
				ActionOpenFile.super.actionPerformed(event);
				// when loading is finished, make frame disappear
				SwingUtilities.invokeLater(() -> dialog.dispose());
			};
			new Thread(runnable).start();
		}
	}

	private static void setLastDirectories(final File file) {
		if (file == null || !file.isDirectory() || !file.exists()) {
			throw new IllegalArgumentException("path is empty" + file);
		}
		String[] dirs =
				VadereConfig.getConfig().getString("recentlyOpenedFiles", "").split(",");
		int maxSavedDirs = CONFIG.getInt("PostVis.maxNumberOfSaveDirectories");

		if (dirs != null) {
			int i = 0;
			String absPath = file.getAbsolutePath();
			StringBuilder paths = new StringBuilder(absPath);
			while (i < dirs.length && i < maxSavedDirs) {
				if (!dirs[i].equals(absPath)) {
					paths.append(",");
					paths.append(dirs[i]);
				}
				i++;
			}
			VadereConfig.getConfig().setProperty("recentlyOpenedFiles", paths.toString());
		}
	}
}
