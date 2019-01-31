package org.vadere.gui.postvisualization.utils;


import org.jcodec.api.awt.SequenceEncoder;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.*;

public class MovRecorder implements IRecorder {
	private static Logger logger = Logger.getLogger(MovRecorder.class);
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
		this.enc = null;
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
		try {
			enc.finish();
		} catch (IndexOutOfBoundsException error) {
			logger.debug("Nothing recorded! " + error.getMessage());
			throw error;
		}
		logger.info(this + " stop recording");
	}

	@Override
	public synchronized void startRecording(final Rectangle2D.Double imageSize) throws IOException {
		try {
			this.imageSize = imageSize;
			this.viewport = model.getViewportBound();
			startRecording();
			logger.info(this + " start recording");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}

	@Override
	public synchronized void startRecording() throws IOException {
		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(resources.getProperty("SettingsDialog.dataFormat"));
		String formattedDate = formatter.format(todaysDate);
		JFileChooser fileChooser = new JFileChooser(Preferences.userNodeForPackage(PostVisualisation.class).get("SettingsDialog.snapshotDirectory.path", "."));
		File outputFile = new File(Messages.getString("FileDialog.filenamePrefix") + formattedDate + ".mov");
		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".mov") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".mov");
			try {
				enc = new SequenceEncoder(outputFile);
			} catch (IOException e) {
				enc = null;
				throw e;
			}
		}
	}

	@Override
	public String toString() {
		return "Mov Recorder";
	}
}
