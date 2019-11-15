package org.vadere.gui.components.control.simulation;


import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.gui.postvisualization.utils.InetEnvironmentGenerator;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;

public class ActionGenerateINETenv extends AbstractAction implements IRendererChangeListener {

	private static Logger logger = Logger.getLogger(ActionGenerateINETenv.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private final InetEnvironmentGenerator generator;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public ActionGenerateINETenv(final String name, final Icon icon, final SimulationRenderer renderer,
								 final SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		this.generator = new InetEnvironmentGenerator(renderer, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Date todaysDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(CONFIG.getString("SettingsDialog.dataFormat"));
		String formattedDate = formatter.format(todaysDate);

		JFileChooser fileChooser = new JFileChooser(VadereConfig.getConfig().getString("SettingsDialog.snapshotDirectory.path", "."));
		File outputFile = new File(Messages.getString("FileDialog.filenamePrefix") + formattedDate + ".xml");

		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".xml") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".xml");

			generator.generateInetEnvironment(outputFile);
			VadereConfig.getConfig().setProperty("SettingsDialog.snapshotDirectory.path", outputFile.getParentFile().getAbsolutePath());
		}
	}

	@Override
	public void update(SimulationRenderer renderer) {
	}
}
