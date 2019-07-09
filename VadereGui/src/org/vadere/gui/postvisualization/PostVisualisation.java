package org.vadere.gui.postvisualization;

import org.vadere.gui.postvisualization.view.PostvisualizationWindow;
import org.vadere.util.logging.Logger;
import org.vadere.util.logging.StdOutErrLog;

public class PostVisualisation {
	// TODO: Move logic to read/write config file to "VadereConfig.java".
	private static Logger logger = Logger.getLogger(PostVisualisation.class);

	public static void main(String[] args) {
		StdOutErrLog.addStdOutErrToLog();
		logger.info("starting post visualization ...");
		// load settings
		logger.info("preferences started");
		// start main gui
		PostvisualizationWindow.start();
		logger.info("post visualization started");
	}
}
