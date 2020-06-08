package org.vadere.gui.postvisualization.utils;


import org.apache.commons.configuration2.Configuration;
import org.jcodec.api.awt.SequenceEncoder;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;

public class MovRecorder implements IRecorder {
	private static Logger logger = Logger.getLogger(MovRecorder.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private final SimulationModel model;
	private ImageGenerator generator;
	private SequenceEncoder enc;
	private double simTimeInSec;
	private Rectangle2D.Double imageSize;
	private Rectangle2D.Double viewport;
	private boolean finished;
	private File outputFile;

	public MovRecorder(final SimulationRenderer renderer) {
		this.model = renderer.getModel();
		this.imageSize = model.getWindowBound();
		this.viewport = model.getViewportBound();
		this.generator = new ImageGenerator(renderer, renderer.getModel());
		this.simTimeInSec = 0.0;
		this.enc = null;
		this.finished = false;
	}

	@Override
	public void update(Observable o, Object arg) {
		synchronized(model) {
			try {
				if (model.config.isRecording() && model.getSimTimeInSec() > simTimeInSec) {
					addPicture();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
				logger.error(ioe.getMessage());
			}
		}
	}

	private void addPicture() throws IOException {
		synchronized(model) {
			if(!this.finished) {
				simTimeInSec = model.getSimTimeInSec();
				Rectangle2D.Double oldViewport = model.getViewportBound();
				model.setViewportBound(viewport);
				BufferedImage bi = generator.generateImage(imageSize);
				logger.info(this + " add picture " + bi.getWidth() + " (width), " + bi.getHeight() + " (height).");
				enc.encodeImage(bi);
				model.setViewportBound(oldViewport);
			}
		}
	}

	@Override
	public void stopRecording() throws IOException {
		synchronized(model) {
			try {
				this.finished = true;
				enc.finish();
				VadereConfig.getConfig().setProperty("SettingsDialog.snapshotDirectory.path", outputFile.getParentFile().getAbsolutePath());
			} catch (IndexOutOfBoundsException error) {
				logger.debug("Nothing recorded! " + error.getMessage());
				throw error;
			}
			logger.info(this + " stop recording");
		}
	}

	@Override
	public void startRecording(final Rectangle2D.Double imageSize) throws IOException {
		synchronized(model) {
			try {
				this.finished = false;
				this.imageSize = imageSize;
				this.viewport = model.getViewportBound();
				this.simTimeInSec = 0;
				startRecording();
				logger.info(this + " start recording");
			} catch (IOException e) {
				e.printStackTrace();
				logger.error(e.getMessage());
			}
		}
	}

	@Override
	public void startRecording() throws IOException {
		synchronized(model) {
			Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(CONFIG.getString("SettingsDialog.dataFormat"));
			String formattedDate = formatter.format(todaysDate);
		JFileChooser fileChooser = new JFileChooser(VadereConfig.getConfig().getString("SettingsDialog.snapshotDirectory.path", "."));
			outputFile = new File(Messages.getString("FileDialog.filenamePrefix") + formattedDate + ".mov");
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
	}

	@Override
	public String toString() {
		return "Mov Recorder";
	}
}
