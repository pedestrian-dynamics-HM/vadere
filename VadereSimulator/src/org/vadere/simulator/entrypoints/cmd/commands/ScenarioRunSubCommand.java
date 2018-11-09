package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.log4j.Logger;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.entrypoints.cmd.SubCommandRunner;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.simulator.utils.scenariochecker.AbstractScenarioCheckerMessageFormatter;
import org.vadere.simulator.utils.scenariochecker.ConsoleScenarioCheckerMessageFormatter;
import org.vadere.simulator.utils.scenariochecker.ScenarioChecker;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.PriorityQueue;

public class ScenarioRunSubCommand implements SubCommandRunner {
	private final static Logger logger = Logger.getLogger(ScenarioRunSubCommand.class);

	private boolean checkScenario(final Scenario scenario, String scenarioCheckerSwitch){

		if(scenarioCheckerSwitch.equals(ScenarioChecker.CHECKER_OFF)){
			return true;
		}

		ScenarioChecker checker = new ScenarioChecker(scenario);
		ConsoleScenarioCheckerMessageFormatter formatter = new ConsoleScenarioCheckerMessageFormatter(scenario);
		PriorityQueue<ScenarioCheckerMessage> msg = checker.checkBuildingStep();
		if (msg.size() > 0){
			System.out.println(formatter.formatMessages(msg));
			ScenarioCheckerMessage firstMsg = msg.peek();
			if (firstMsg != null && firstMsg.isError()){
				return false;
			}
		}
		return true;
	}

	@Override
	public void run(Namespace ns, ArgumentParser parser) {
		Path outputDir = Paths.get(ns.getString("output-dir"));
		String scenarioCheckerSwitch = ns.getString("scenario-checker");
		if (!outputDir.toFile().exists()){
			if ( ! outputDir.toFile().mkdirs() ) {
				logger.error("Could not create all necessary directories: " + outputDir.toFile().toString());
				System.exit(-1);
			} else {
				logger.info("Created output directory: " + outputDir.toAbsolutePath().toFile().toString());
			}
		} else {
			logger.info("Use output directory: " + outputDir.toAbsolutePath().toFile().toString());
		}

		Path scenarioFile = Paths.get(ns.getString("scenario-file"));
		if (!scenarioFile.toFile().exists() || !scenarioFile.toFile().isFile()){
			logger.error("scenario-file does not exist, is not a regular file or you do not have read permissions: "
					+ scenarioFile.toFile().toString());
			System.exit(-1);
		}

		try {
			Scenario scenario = ScenarioFactory.createScenarioWithScenarioFilePath(scenarioFile);
			if (checkScenario(scenario, scenarioCheckerSwitch)){
				new ScenarioRun(scenario, outputDir.toFile().toString() , null).run();
			} else {
				System.exit(-1);
			}
		} catch (Exception e){
			logger.error(e);
			System.exit(-1);
		}

	}
}
