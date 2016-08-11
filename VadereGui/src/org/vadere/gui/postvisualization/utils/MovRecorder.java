package org.vadere.gui.postvisualization.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jcodec.api.awt.SequenceEncoder;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.prefs.Preferences;

public class MovRecorder implements IRecorder {
	private static Logger logger = LogManager.getLogger(MovRecorder.class);
	private static Resources resources = Resources.getInstance("postvisualization");

	private final PostvisualizationModel model;
	private ImageGenerator generator;
	private SequenceEncoder enc;
	private int step;
	private Rectangle2D.Double imageSize;
	private Rectangle2D.Double viewport;

	public MovRecorder(final PostvisualizationRenderer renderer) {
		this.model = renderer.getModel();
		this.imageSize = model.getWindowBound();
		this.viewport = model.getViewportBound();
		this.generator = new ImageGenerator(renderer, renderer.getModel());
		this.step = 0;
	}

	@Override
	public synchronized void update(Observable o, Object arg) {
		try {
			if (model.config.isRecording() && model.getStep().isPresent()
					&& model.getStep().get().getStepNumber() != step) {
				step = model.getStep().get().getStepNumber();
				addPicture();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			logger.error(ioe.getMessage());
		}
	}

	private synchronized void addPicture() throws IOException {
		Rectangle2D.Double oldViewport = model.getViewportBound();
		model.setViewportBound(viewport);
		BufferedImage bi = generator.generateImage(imageSize);
		enc.encodeImage(bi);
		logger.info(this + " add picture");
		model.setViewportBound(oldViewport);
	}

	@Override
	public synchronized void stopRecording() throws IOException {
		enc.finish();
		logger.info(this + " stop recording");
	}

	@Override
	public synchronized void startRecording(final Rectangle2D.Double imageSize) throws IOException {
		this.imageSize = imageSize;
		this.viewport = model.getViewportBound();
		startRecording();
	}

	@Override
	public synchronized void startRecording() {
		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(resources.getProperty("View.dataFormat"));
		String formattedDate = formatter.format(todaysDate);
		try {
			this.enc = new SequenceEncoder(new File(
					Preferences.userNodeForPackage(PostVisualisation.class).get("PostVis.snapshotDirectory.path", ".")
							+ System.getProperty("file.separator") + "pv_snapshot_" + formattedDate + ".mov"));
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		logger.info(this + " start recording");
	}

	@Override
	public String toString() {
		return "Mov Recorder";
	}
}
