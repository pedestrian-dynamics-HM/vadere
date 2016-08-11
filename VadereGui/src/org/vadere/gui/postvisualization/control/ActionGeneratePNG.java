package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.utils.ImageGenerator;
import org.vadere.gui.postvisualization.view.ImageSizeDialog;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;

public class ActionGeneratePNG extends ActionVisualization {
	private static Logger logger = LogManager.getLogger(ActionGeneratePNG.class);
	private static Resources resources = Resources.getInstance("postvisualization");
	private ImageGenerator generator;

	public ActionGeneratePNG(final String name, Icon icon, final PostvisualizationRenderer renderer) {
		super(name, icon, renderer.getModel());
		generator = new ImageGenerator(renderer, renderer.getModel());
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
}
