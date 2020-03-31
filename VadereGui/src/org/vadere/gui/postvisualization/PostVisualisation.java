package org.vadere.gui.postvisualization;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.postvisualization.view.PostvisualizationWindow;
import org.vadere.gui.projectview.VadereApplication;
import org.vadere.util.io.VadereArgumentParser;
import org.vadere.util.logging.Logger;
import org.vadere.util.logging.StdOutErrLog;

public class PostVisualisation {
	private static Logger logger = Logger.getLogger(PostVisualisation.class);

	public static void main(String[] args) {
		StdOutErrLog.addStdOutErrToLog();
		logger.info("starting Vadere PostVisualization...");

		VadereArgumentParser vadereArgumentParser = new VadereArgumentParser();
		ArgumentParser argumentParser = vadereArgumentParser.getArgumentParser();

		try {
			vadereArgumentParser.parseArgsAndProcessInitialOptions(args);
		} catch (UnsatisfiedLinkError linkError) {
			System.err.println("[LWJGL]: " + linkError.getMessage());
		} catch (ArgumentParserException e) {
			argumentParser.handleError(e);
			System.exit(1);
		} catch (Exception e) {
			System.err.println("Cannot start vadere: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		Messages.loadLanguageFromPreferences(VadereApplication.class);

		PostvisualizationWindow.start();
	}
}
