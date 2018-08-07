package org.vadere.simulator.entrypoints;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.apache.log4j.Logger;
/**
 * Provides the possibility to start VADERE in console mode.
 * 
 */
public class VadereConsole {

	private final static Logger logger = Logger.getLogger(VadereConsole.class);

	public static void main(String[] args) {
		ArgumentParser parser = createArgumentParser();

//		args = new String[]{"migrate", "-f", "/home/lphex/hm.d/vadere/VadereSimulator/testResources/data/simpleProject/output/test_postvis_2018-01-17_16-56-37.307/test_postvis.scenario"};
		try {
			Namespace ns = parser.parseArgs(args);
			SubCommandRunner sRunner = ns.get("func");
			sRunner.run(ns, parser);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		} catch (Exception e) {
			logger.error("error in command:" + e.getMessage());
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
				.addParser(SubCommand.PROJECT_RUN.getCmdName())
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

		// Run Scenario
		Subparser scenarioRun = subparsers
				.addParser(SubCommand.SCENARO_RUN.getCmdName())
				.help("Run scenario without a project")
				.setDefault("func", new ScenarioRunSubCommand());
		scenarioRun.addArgument("--output-dir", "-o")
				.required(false)
				.setDefault("output")
				.dest("output-dir") // set name in namespace
				.type(String.class)
				.help("Supply differernt output directory path to use.");

		scenarioRun.addArgument("--scenario-file", "-f")
				.required(true)
				.type(String.class)
				.dest("scenario-file")
				.help("Scenario files to run");

		// Run SUQ
		Subparser suqRun = subparsers
				.addParser(SubCommand.SUQ.getCmdName())
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
				.help("Scenario files to run");


		// Run Migration Assistant
		Subparser migrationAssistant = subparsers
				.addParser(SubCommand.MIGRATE.getCmdName())
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

		migrationAssistant.addArgument("--revert-migration")
				.required(false)
				.action(Arguments.storeTrue())
				.dest("revert-migration")
				.help("if set vadere will search for a <scenario-file>.legacy and will replace the current version with this backup." +
						" The Backup must be in the same directory");

		return parser;
	}

}
