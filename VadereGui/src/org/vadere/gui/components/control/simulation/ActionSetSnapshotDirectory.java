package org.vadere.gui.components.control.simulation;

import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.util.config.VadereConfig;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ActionSetSnapshotDirectory extends ActionVisualization {
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private final JTextField textField;
	private final JDialog parent;

	public ActionSetSnapshotDirectory(final String name, final SimulationModel<? extends DefaultSimulationConfig> model,
			final JTextField textField, final JDialog parent) {
		super(name, model);
		this.textField = textField;
		this.parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final JFileChooser fc = new JFileChooser(CONFIG.getString("SettingsDialog.snapshotDirectory.path"));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = fc.showOpenDialog(parent);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();

			CONFIG.setProperty("SettingsDialog.snapshotDirectory.path", file.getAbsolutePath());
			textField.setText(file.getAbsolutePath());
		}
	}

}
