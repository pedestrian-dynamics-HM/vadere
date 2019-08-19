package org.vadere.gui.components.control.simulation;


import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.gui.postvisualization.utils.SVGGenerator;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActionGenerateSVG extends AbstractAction implements IRendererChangeListener {
	private static Logger logger = Logger.getLogger(ActionGenerateSVG.class);

	private final SVGGenerator svgGenerator;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public ActionGenerateSVG(final String name, final Icon icon, final SimulationRenderer renderer,
			final SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		this.svgGenerator = new SVGGenerator(renderer, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(VadereConfig.getConfig().getString("SettingsDialog.dataFormat"));
		String formattedDate = formatter.format(todaysDate);

		JFileChooser fileChooser = new JFileChooser(VadereConfig.getConfig().getString("SettingsDialog.snapshotDirectory.path", "."));
		File outputFile = new File(Messages.getString("FileDialog.filenamePrefix") + formattedDate + ".svg");

		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".svg") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".svg");
			svgGenerator.generateSVG(outputFile);
			VadereConfig.getConfig().setProperty("SettingsDialog.snapshotDirectory.path", outputFile.getParentFile().getAbsolutePath());
		}

	}

	@Override
	public void update(SimulationRenderer renderer) {}
}
