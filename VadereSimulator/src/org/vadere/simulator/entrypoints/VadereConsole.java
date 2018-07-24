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



		System.out.println(System.getProperty("user.dir"));

		Locale.setDefault(Locale.ENGLISH);
		String pathToScenarioFile = ns.getString("scenario-file");
		String outputDir = ns.get("output-dir");
		if (ns.getBoolean("suq")){
			if (outputDir == null){
				throw new IllegalArgumentException("If the option -suq is activated, an output directory (output-dir) has to be specified!");
			}
		}

		if (pathToScenarioFile == null) {
			System.err.println("Too few arguments. Exiting.");
			parser.printHelp();
			System.exit(1);
		}

		String scenarioFilePath = Paths.get(pathToScenarioFile).toString();
		String scenarioFile = Paths.get(pathToScenarioFile).getFileName().toString();
		String projectDirectory = Paths.get(ns.getString("scenario-file")).getParent().toString();

		if(!Files.exists(Paths.get(projectDirectory, scenarioFile))) {
			System.err.println("The file " + Paths.get(projectDirectory, scenarioFile) + " does not exist");
		}

		logger.info(String.format("Running VADERE on %s...", scenarioFilePath));

		try {
			Scenario scenario;
			if (ns.getBoolean("suq")) {
				 scenario = ScenarioFactory.createScenarioWithScenarioFilePath(Paths.get(scenarioFile));
			}else{
				scenario = ScenarioFactory.createVadereWithProjectDirectory(scenarioFile, projectDirectory);
			}
			if(outputDir != null) {
				if (ns.getBoolean("suq")){
					new ScenarioRun(scenario, outputDir, null,true).run();
				}else {
					new ScenarioRun(scenario, outputDir, null).run();
				}

			}
			else {
				if (ns.getBoolean("suq")){
					new ScenarioRun(scenario, null, null,true).run();
				}else {
					new ScenarioRun(scenario, null).run();
				}
			}

		} catch (Exception e) {
			logger.error(e);
			System.exit(-1);
		}
	}

	private static ArgumentParser createArgumentParser() {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("Vadere")
				.defaultHelp(true)
				.description("Runs the VADERE pedestrian simulator.");
		parser.addArgument("scenario-file").required(true)
				.help("Path to the scenario file.");
		parser.addArgument("output-dir").required(false)
				.help("Path to the directory of the output. By default this is ./output of the directory of the executable.");
		parser.addArgument("-suq").required(false)
				.help("Indicates that the folder structure for input and output is fully controlled by the user. Only a single scenario is run, no project or project structure is necessary. Intended for the SUQ-Controller. Outputpath has to be specified!")
				.action(Arguments.storeTrue());
		return parser;
	}

}
