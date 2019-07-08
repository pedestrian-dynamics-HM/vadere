package org.vadere.gui.projectview;


import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;
import org.vadere.util.logging.StdOutErrLog;

/**
 * Entry point for the Vadere GUI.
 * 
 * 
 */
public class VadereApplication {

	private static Logger logger = Logger.getLogger(VadereApplication.class);

	public static void main(String[] args) {
		StdOutErrLog.addStdOutErrToLog();
		logger.info("starting Vadere GUI...");

		// set locale
		Messages.loadLanguageFromPreferences(VadereApplication.class);

		// start main gui
		ProjectView.start();
	}

}
