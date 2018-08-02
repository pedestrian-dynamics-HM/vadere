package org.vadere.simulator.entrypoints;

import org.apache.log4j.Logger;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.simulator.projects.migration.incidents.VersionBumpIncident;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import org.vadere.simulator.entrypoints.Version;
/**
 * Provides the possibility to start VADERE in console mode.
 * 
 */
public class VadereConsole {

	private final static Logger logger = Logger.getLogger(VadereConsole.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws ArgumentParserException {
		ArgumentParser parser = createArgumentParser();

//		args = new String[]{"migrate", "-f", "/home/lphex/hm.d/vadere/VadereSimulator/testResources/data/simpleProject/output/test_postvis_2018-01-17_16-56-37.307/test_postvis.scenario"};
		try {
			Namespace ns = parser.parseArgs(args);
			SubCommandRunner sRunner = (SubCommandRunner) ns.get("func");
			sRunner.run(ns, parser);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

	}

	private static ArgumentParser createArgumentParser() {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("Vadere")
				.defaultHelp(true)
				.description("Runs the VADERE pedestrian simulator.");
		Subparsers subparsers = parser.addSubparsers()
										.title("subcommands")
										.description("valid subcommands")
										.metavar("COMMAND");

		// Run Project
		Subparser projectRun = subparsers
				.addParser("project-run")
				.help("This command uses a Vadere Project and runs selected scenario.")
				.setDefault("func", new ProjectRunSubCommand());
		projectRun.addArgument("--project-dir", "-p")
				.required(true)
				.type(String.class)
				.dest("project-dir")
				.help("Path to project directory");
		projectRun.addArgument("--scenario-file", "-f")
				.required(true)
				.type(String.class)
				.dest("scenario-file")
				.help("Name of Scenario file");


		// Run SUQ
		Subparser suqRun = subparsers
				.addParser("suq")
				.help("Run a single scenario file to specify to  fully controll folder structure for input and output.")
				.setDefault("func", new SuqSubCommand());

		suqRun.addArgument("--output-dir", "-o")
				.required(false)
				.setDefault("output")
				.dest("output-dir") // set name in namespace
				.type(String.class)
				.help("Supply differernt output directory path to use.");

		suqRun.addArgument("--scenario-file", "-f")
				.required(true)
				.type(String.class)
				.dest("scenario-file")
				.help("List of scenario files to run");


		// Run Migration Assistant
		Subparser migrationAssistant = subparsers
				.addParser("migrate")
				.help("Run migration assistant on single sceanrio file")
				.setDefault("func", new MigrationSubCommand());

		migrationAssistant.addArgument("--scenario-file", "-f")
				.required(true)
				.type(String.class)
				.dest("scenario-file")
				.help("The scenario file which should be migrated to new version");

		String[] versions = Version.stringValues(Version.NOT_A_RELEASE);
		migrationAssistant.addArgument("--target-version", "-V")
				.required(false)
				.type(String.class)
				.dest("target-version")
				.choices(versions)
				.setDefault(Version.latest().label())
				.help("use one of the shown version strings to indicate the target version. If not specified the last version is used");

		migrationAssistant.addArgument("--output-file", "-o")
				.required(false)
				.type(String.class)
				.dest("output-file")
				.choices(versions)
				.help("Write new version to this file. If not specified backup input file and overwrite it.");

		return parser;
	}

}
