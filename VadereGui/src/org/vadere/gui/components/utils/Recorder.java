package org.vadere.gui.components.utils;


import org.apache.commons.configuration2.Configuration;
import org.jcodec.api.awt.SequenceEncoder;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.postvisualization.utils.IRecorder;
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

public class Recorder implements IRecorder {

	private static Logger logger = Logger.getLogger(Recorder.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private SequenceEncoder enc;

	@Override
	public void startRecording() {
		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(CONFIG.getString("SettingsDialog.dataFormat"));
		String formattedDate = formatter.format(todaysDate);
		JFileChooser fileChooser = new JFileChooser(VadereConfig.getConfig().getString("SettingsDialog.snapshotDirectory.path", "."));
		File outputFile = new File("VADERE_sim_" + formattedDate + ".mov");
		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".mov") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".mov");
			try {
				this.enc = new SequenceEncoder(outputFile);
			} catch (IOException e) {
				e.printStackTrace();
				logger.error(e.getMessage());
			}
			logger.info(this + " start recording");
		}
	}

	public synchronized void addPicture(@NotNull final BufferedImage bi) throws IOException {
		enc.encodeImage(bi);
		logger.info(this + " add picture");
	}

	@Override
	public void startRecording(Rectangle2D.Double imageSize) {
		startRecording();
	}

	@Override
	public void stopRecording() throws IOException {
		try {
			enc.finish();
		} catch (IndexOutOfBoundsException error) {
			logger.debug( "Nothing recorded! %s" , error.getMessage());
		}
		logger.info(this + " stop recording");
	}

	@Override
	public void update(Observable o, Object arg) {}
}
