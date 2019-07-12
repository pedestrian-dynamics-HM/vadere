package org.vadere.gui.components.control.simulation;


import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.gui.postvisualization.utils.TikzGenerator;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActionGenerateTikz extends AbstractAction implements IRendererChangeListener {

	private static Logger logger = Logger.getLogger(ActionGenerateTikz.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private final TikzGenerator tikzGenerator;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public ActionGenerateTikz(final String name, final Icon icon, final SimulationRenderer renderer,
							  final SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		this.tikzGenerator = new TikzGenerator(renderer, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Date todaysDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(CONFIG.getString("SettingsDialog.dataFormat"));
		String formattedDate = formatter.format(todaysDate);

		JFileChooser fileChooser = new JFileChooser(VadereConfig.getConfig().getString("SettingsDialog.snapshotDirectory.path", "."));
		File outputFile = new File(Messages.getString("FileDialog.filenamePrefix") + formattedDate + ".tex");

		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".tex") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".tex");

			tikzGenerator.generateTikz(outputFile);
		}
	}

	@Override
	public void update(SimulationRenderer renderer) {
	}
}
