package org.vadere.gui.onlinevisualization.control;

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

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;

public class ActionGeneratePNG extends AbstractAction implements IRendererChangeListener {
	private static Logger logger = LogManager.getLogger(ActionGeneratePNG.class);
	private static Resources resources = Resources.getInstance("postvisualization");
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
			BufferedImage bi = generator.generateImage(imageSizeDialog.getImageBound());

			Date todaysDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat(resources.getProperty("View.dataFormat"));
			String formattedDate = formatter.format(todaysDate);

			File outputfile = new File(
					Preferences.userNodeForPackage(PostVisualisation.class).get("PostVis.snapshotDirectory.path", ".")
							+ System.getProperty("file.separator") + "pv_snapshot_" + formattedDate + ".png");
			try {
				ImageIO.write(bi, "png", outputfile);
				logger.info("generate new png: " + outputfile.getAbsolutePath());
			} catch (IOException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void update(SimulationRenderer renderer) {
		this.generator = new ImageGenerator(renderer, model);
	}
}
