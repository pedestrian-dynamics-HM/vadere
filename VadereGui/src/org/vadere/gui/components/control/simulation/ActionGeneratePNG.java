package org.vadere.gui.components.control.simulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.utils.ImageGenerator;
import org.vadere.gui.postvisualization.view.ImageSizeDialog;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.*;

public class ActionGeneratePNG extends AbstractAction implements IRendererChangeListener {
	private static Logger logger = LogManager.getLogger(ActionGeneratePNG.class);
	private static Resources resources = Resources.getInstance("global");
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
			JFileChooser fileChooser = new JFileChooser(Preferences.userNodeForPackage(PostVisualisation.class).get("SettingsDialog.snapshotDirectory.path", "."));

			Date todaysDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat(resources.getProperty("SettingsDialog.dataFormat"));
			String formattedDate = formatter.format(todaysDate);


			File outputFile = new File("pv_snapshot_" + formattedDate + ".png");
			fileChooser.setSelectedFile(outputFile);

			int returnVal = fileChooser.showDialog(null, "Save");

			if (returnVal == JFileChooser.APPROVE_OPTION) {

				outputFile = fileChooser.getSelectedFile().toString().endsWith(".png") ? fileChooser.getSelectedFile()
						: new File(fileChooser.getSelectedFile().toString() + ".png");

				BufferedImage bi = generator.generateImage(imageSizeDialog.getImageBound());

				try {
					ImageIO.write(bi, "png", outputFile);
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
