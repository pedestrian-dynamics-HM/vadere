package org.vadere.gui.postvisualization.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.model.DefaultModel;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class ImageGenerator<T extends DefaultSimulationConfig> {

	private static Logger logger = LogManager.getLogger(ImageGenerator.class);
	private SimulationRenderer renderer;
	private SimulationModel<? extends DefaultSimulationConfig> model;

	public ImageGenerator(final SimulationRenderer renderer,
			final SimulationModel<? extends DefaultSimulationConfig> model) {
		this.renderer = renderer;
		this.model = model;
	}

	public BufferedImage generateImage(final double scaleFactor) {
		BufferedImage bi = null;
		synchronized (renderer) {
			double oldScale = model.getScaleFactor();
			try {
				model.setScale(scaleFactor);
				bi = renderer.renderImage(ImageGenerator.calculateOptimalWidth(model),
						ImageGenerator.calculateOptimalHeight(model));
			} catch (Exception e) {
				logger.error("could not render image " + e.getMessage());
			} finally {
				model.setScale(oldScale);
			}
		}
		return bi;
	}

	public BufferedImage generateImage(final Rectangle2D.Double imageSize) {
		return generateImage(imageSize.getWidth() / model.getTopographyBound().getWidth());
	}

	public BufferedImage generateImage() {
		return generateImage(model.getScaleFactor());
	}

	public static int calculateOptimalWidth(final DefaultModel model) {
		return calculateOptimalWidth(model, model.getScaleFactor());
	}


	public static int calculateOptimalWidth(final DefaultModel model, final double scale) {
		Rectangle2D.Double viewportBound = model.getViewportBound();
		Rectangle2D.Double topographyBound = model.getTopographyBound();

		int w = (int) Math.min((viewportBound.getWidth()) * scale, topographyBound.getWidth() * scale);

		return w;
	}

	public static int calculateOptimalHeight(final DefaultModel model, final double scale) {
		Rectangle2D.Double viewportBound = model.getViewportBound();
		Rectangle2D.Double topographyBound = model.getTopographyBound();

		int h = (int) Math.min((viewportBound.getHeight()) * scale, topographyBound.getHeight() * scale);

		return h;
	}

	public static int calculateOptimalHeight(final DefaultModel model) {
		return calculateOptimalHeight(model, model.getScaleFactor());
	}
}
