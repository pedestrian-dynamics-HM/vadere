package org.vadere.gui.components.control.simulation;

import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.gui.postvisualization.utils.ImageGenerator;
import org.vadere.gui.postvisualization.view.ImageSizeDialog;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActionGeneratePNG extends AbstractAction implements IRendererChangeListener {

	private static Logger logger = Logger.getLogger(ActionGeneratePNG.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private ImageGenerator generator;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public ActionGeneratePNG(final String name, Icon icon, final SimulationRenderer renderer,
			final SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		generator = new ImageGenerator(renderer, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {

		ImageSizeDialog imageSizeDialog = new ImageSizeDialog(model);

		if (imageSizeDialog.getState() == ImageSizeDialog.State.Ok) {
			JFileChooser fileChooser = new JFileChooser(VadereConfig.getConfig().getString("SettingsDialog.snapshotDirectory.path", "."));

			Date todaysDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat(CONFIG.getString("SettingsDialog.dataFormat"));
			String formattedDate = formatter.format(todaysDate);

			File outputFile = new File(Messages.getString("FileDialog.filenamePrefix") + formattedDate + ".png");
			fileChooser.setSelectedFile(outputFile);

			int returnVal = fileChooser.showDialog(null, "Save");

			if (returnVal == JFileChooser.APPROVE_OPTION) {

				outputFile = fileChooser.getSelectedFile().toString().endsWith(".png") ? fileChooser.getSelectedFile()
						: new File(fileChooser.getSelectedFile().toString() + ".png");

				BufferedImage bi = generator.generateImage(imageSizeDialog.getImageBound());

				try {
					ImageIO.write(bi, "png", outputFile);
					VadereConfig.getConfig().setProperty("SettingsDialog.snapshotDirectory.path", outputFile.getParentFile().getAbsolutePath());
					logger.info("generate new png: " + outputFile.getAbsolutePath());
				} catch (IOException e1) {
					logger.error(e1.getMessage());
					e1.printStackTrace();
				}
			}
		}
	}

	@Override
	public void update(SimulationRenderer renderer) {
		this.generator = new ImageGenerator(renderer, model);
	}
}
