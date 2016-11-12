package org.vadere.simulator.entrypoints;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.util.io.IOUtils;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Provides the possibility to start VADERE in console mode.
 * 
 */
public class VadereConsole {

	private final static Logger logger = Logger.getLogger(VadereConsole.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArgumentParser parser = createArgumentParser();

		Namespace ns = null;
		try {
			ns = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

		Locale.setDefault(Locale.ENGLISH);

		String scenarioFilePath = Paths.get(ns.getString("scenario-file")).toString();
		String vadereName = Paths.get(ns.getString("scenario-file")).getFileName().toString().replace(".scenario", "");
		String projectDirectory = ns.getString("out_path");
		String outputFile = ns.getString("output-file");
		String lockDirectory = ns.getString("lock-folder");
		String timeStepFile = ns.getString("time-step-file");
		Boolean outputAll = ns.getBoolean("all");

		if ((lockDirectory == null || outputFile == null || timeStepFile == null) && projectDirectory == null) {
			System.err.println("Too few arguments. Exiting.");
			parser.printHelp();
			System.exit(1);
		}

		if (lockDirectory != null && outputFile != null && projectDirectory == null) {
			if (!Files.exists(Paths.get(outputFile))) {
				try {
					Files.createFile(Paths.get(outputFile));
				} catch (IOException e) {
					logger.error(e);
					System.exit(1);
				}
			}
		}

		logger.info(String.format("Running VADERE on %s...", scenarioFilePath));

		try {
			Scenario scenario = ScenarioFactory.createVadereWithProjectDirectory(projectDirectory,
					vadereName + IOUtils.SCENARIO_FILE_EXTENSION, vadereName);
			new ScenarioRun(scenario).run();
		} catch (IOException e) {
			logger.error(e);
		}
	}

	private static ArgumentParser createArgumentParser() {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("Vadere")
				.defaultHelp(true)
				.description("Runs the VADERE pedestrian simulator.");
		parser.addArgument("scenario-file").required(true)
				.help("Path to the scenario file.");
		parser.addArgument("lock-folder")
				.help("Path to the folder containing lock.lck files.");
		parser.addArgument("time-step-file")
				.help("Path to the time step file.");
		parser.addArgument("output-file")
				.help("Path to the output file.");

		MutuallyExclusiveGroup modesGroup = parser.addMutuallyExclusiveGroup("running mode").required(true);
		modesGroup.description(
				"Starts VADERE in [normal] or [lock] mode.\nIn [normal] mode, it runs the given scenario-file" +
						" once and then exits.\nIn [lock] mode, it creates a lock.lck file in the given lock-folder and then waits."
						+
						" After the lock.lck is deleted, the scenario is simulated until the finish time is reached. The lock.lck file is created"
						+
						" and Vadere waits for its deletion again.");

		modesGroup.addArgument("-op", "--out-path")
				.help("NOT in [lock] mode! Path to the output folder where VADERE store additional results of the simulation.");

		modesGroup.addArgument("-mtv").type(Boolean.class).action(Arguments.storeTrue())
				.help("Initializes the [lock] mode. Needs three strings: 1. path to folder where the lock.lck file is created, 2. path to time step file, 3. path to output file.");

		// modesGroup.addArgument("-of", "--out-file")
		// .help("Path to the output file where VADERE appends results of the simulation.");

		// parser.addArgument("-tsf", "--time-step-file")
		// .help("Path to the time step file where VADERE store and reads intermediate results.");

		// parser.addArgument("-l","--lock")
		// .help("Path to the .lock files folder.");

		parser.addArgument("-all").type(Boolean.class).action(Arguments.storeTrue())
				.help("Output all computed positions, not just the ones after the time step.");

		ArgumentGroup formatgroup = parser.addArgumentGroup("format");
		formatgroup.addArgument("-outformat", "--output-format")
				.help("Format used in the output file.")
				.setDefault("%d;%f;%f;%f;%f;%d;%d;%f;");
		formatgroup.addArgument("-outvars", "--output-variables")
				.help("Variables used in the output file, separated by comma, no spaces. The --output-format must define the format in the same order.")
				.setDefault("id,lastX,lastY,lastVX,lastVY,targetId,sourceId,desiredSpeed");

		formatgroup.addArgument("-tsfformat", "--tsf-format")
				.help("Format used in the time step file.")
				.setDefault("%d;%f;%f;%f;%f;%d;%d;%f");
		formatgroup.addArgument("-tsfvars", "--tsf-variables")
				.help("Variables used in the output file, separated by comma, no spaces. The --tsf-format must define the format in the same order.")
				.setDefault("id,lastX,lastY,lastVX,lastVY,targetId,sourceId,desiredSpeed");
		return parser;
	}

}
