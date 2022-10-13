package org.vadere.gui.projectview;


import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeModelCache;
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
		argumentParser.addArgument("--project")
				.required(false)
				.type(String.class)
				.dest("project-path")
				.setDefault("")
				.help("Path to project to open");

		Namespace ns;
		try {
			ns = vadereArgumentParser.parseArgsAndProcessInitialOptions(args);
			Messages.loadLanguageFromPreferences(VadereApplication.class);
			TreeModelCache.buildTreeModelCache();
			ProjectView.start(ns.getString("project-path"));
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




	}

}
