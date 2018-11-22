package org.vadere.simulator.entrypoints.cmd;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.apache.log4j.Logger;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.entrypoints.cmd.commands.MigrationSubCommand;
import org.vadere.simulator.entrypoints.cmd.commands.ProjectRunSubCommand;
import org.vadere.simulator.entrypoints.cmd.commands.ScenarioRunSubCommand;
import org.vadere.simulator.entrypoints.cmd.commands.SetLogLevelCommand;
import org.vadere.simulator.entrypoints.cmd.commands.SuqSubCommand;
import org.vadere.simulator.utils.scenariochecker.ScenarioChecker;

/**
 * Provides the possibility to start VADERE in console mode.
 * 
 */
public class VadereConsole {

	private final static Logger logger = Logger.getLogger(VadereConsole.class);

	public static void main(String[] args) {
		ArgumentParser parser = createArgumentParser();

//		args = new String[]{"--loglevel", "INFO", "scenario-run", "-o", "/tmp", "-f", "/home/lphex/hm.d/vadere/VadereModelTests/TestStairs/scenarios/stairs_diagonal_1_+1.scenario"};
//		args = new String[]{"-h"};

		try {
			Namespace ns = parser.parseArgs(args);
			SubCommandRunner sRunner = ns.get("func");
			sRunner.run(ns, parser);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		} catch (Exception e) {
			logger.error("topographyError in command:" + e.getMessage());
			System.exit(1);
		}

	}

	private static ArgumentParser createArgumentParser() {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("Vadere")
				.defaultHelp(true)
				.description("Runs the VADERE pedestrian simulator.");

		parser.addArgument("--loglevel")
				.required(false)
				.type(String.class)
				.dest("loglevel")
				.choices("OFF", "FATAL", "TOPOGRAPHY_ERROR", "TOPOGRAPHY_WARN", "INFO", "DEBUG", "ALL")
				.setDefault("INFO")
				.action(new SetLogLevelCommand())
				.help("Set Log Level for vadere.");

		Subparsers subparsers = parser.addSubparsers()
										.title("subcommands")
										.description("valid subcommands")
										.metavar("COMMAND");

		// Run Project
		Subparser projectRun = subparsers
				.addParser(SubCommand.PROJECT_RUN.getCmdName())
				.help("This command uses a VADERE project and runs selected scenario.")
				.setDefault("func", new ProjectRunSubCommand());
		projectRun.addArgument("--project-dir", "-p")
				.required(true)
				.type(String.class)
				.dest("project-dir")
				.help("Path to project directory");
		projectRun.addArgument("--scenario-file", "-f")
				.required(false)
				.type(String.class)
				.dest("scenario-file")
				.help("Name of Scenario file");
		projectRun.addArgument("--scenario-checker")
				.required(false)
				.type(String.class)
				.dest("scenario-checker")
				.choices(ScenarioChecker.CHECKER_OFF, ScenarioChecker.CHECKER_OFF)
				.setDefault(ScenarioChecker.CHECKER_OFF)
				.help("Turn Scenario Checker on or off.");

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
				.help("Supply different output directory path to use.");
		scenarioRun.addArgument("--override-timestep-setting")
				.dest("override-timestep-setting")
				.required(false)
				.setDefault(false)
				.action(Arguments.storeTrue())
				.help("This will ignore the TimestampSetting in the scenario file.");

		scenarioRun.addArgument("--scenario-file", "-f")
				.required(true)
				.type(String.class)
				.dest("scenario-file")
				.help("Scenario file to run");
		scenarioRun.addArgument("--scenario-checker")
				.required(false)
				.type(String.class)
				.dest("scenario-checker")
				.choices(ScenarioChecker.CHECKER_OFF, ScenarioChecker.CHECKER_OFF)
				.setDefault(ScenarioChecker.CHECKER_ON)
				.help("Turn Scenario Checker on or off.");

		// Run SUQ
		Subparser suqRun = subparsers
				.addParser(SubCommand.SUQ.getCmdName())
				.help("Run a single scenario file but specify output path manually.")
				.setDefault("func", new SuqSubCommand());

		suqRun.addArgument("--output-dir", "-o")
				.required(false)
				.setDefault("output")
				.dest("output-dir") // set name in namespace
				.type(String.class)
				.help("Supply different output directory path to use.");

		suqRun.addArgument("--scenario-file", "-f")
				.required(true)
				.type(String.class)
				.dest("scenario-file")
				.help("Scenario files to run");


		// Run Migration Assistant
		Subparser migrationAssistant = subparsers
				.addParser(SubCommand.MIGRATE.getCmdName())
				.help("Run migration assistant on single sceanrio file.")
				.setDefault("func", new MigrationSubCommand());

		migrationAssistant.addArgument("path")
				.nargs("+")
				.metavar("PATH")
				.required(true)
				.type(String.class)
				.dest("paths")
				.help("The scenario files or directories on to operate on. Directories containing" +
						"the files DO_NOT_MIGRATE or DO_NOT_MIGRATE_TREE will be ignored.");

		String[] versions = Version.stringValues(Version.NOT_A_RELEASE);
		migrationAssistant.addArgument("--target-version")
				.required(false)
				.type(String.class)
				.dest("target-version")
				.choices(versions)
				.setDefault(Version.latest().label())
				.help("Default: " + Version.latest().label() + " Use one of the shown version strings to indicate the target version." +
						" If not specified the last version is used." );

		migrationAssistant.addArgument("--output-file", "-o")
				.required(false)
				.type(String.class)
				.metavar("OUTPUT-PATH")
				.dest("output-path")
				.help("Write new version to this directory. If not specified backup input file and overwrite.");

		migrationAssistant.addArgument("--revert-migration")
				.required(false)
				.action(Arguments.storeTrue())
				.dest("revert-migration")
				.help("If set vadere will search for a <scenario-file>.legacy and will replace the current version with this backup." +
						" The Backup must be in the same directory");

		migrationAssistant.addArgument("--recursive", "-r")
				.required(false)
				.action(Arguments.storeTrue())
				.dest("recursive")
				.setDefault(false)
				.help("If PATH contains a directory instead of a scenario file recursively search " +
						"the directory tree for scenario files and apply the command");

		migrationAssistant.addArgument("--create-new-version")
				.required(false)
				.type(String.class)
				.dest("create-new-version")
				.help("Create new transformation and identity file based on current latest version" +
						"PATH must point to the directory containing the old transformation files." +
						" This Argument takes the new Version Label as input");

		return parser;
	}

}
