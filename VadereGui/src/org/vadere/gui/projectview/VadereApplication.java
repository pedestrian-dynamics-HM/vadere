package org.vadere.gui.projectview;


import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.util.io.VadereArgumentParser;
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

		VadereArgumentParser vadereArgumentParser = new VadereArgumentParser();
		ArgumentParser argumentParser = vadereArgumentParser.getArgumentParser();

		try {
			vadereArgumentParser.parseArgsAndProcessOptions(args);
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

		ProjectView.start();
	}

}
