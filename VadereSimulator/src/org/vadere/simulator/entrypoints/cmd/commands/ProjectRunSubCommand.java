package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.log4j.Logger;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.entrypoints.cmd.SubCommandRunner;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.util.io.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class ProjectRunSubCommand implements SubCommandRunner {
	private final static Logger logger = Logger.getLogger(ProjectRunSubCommand.class);

	@Override
	public void run(Namespace ns, ArgumentParser parser) {
		Locale.setDefault(Locale.ENGLISH);
		String projectPath = ns.getString("project-dir");
		String scenarioFile = ns.getString("scenario-file");

		Path projectDirectory = Paths.get(projectPath);
		Path scenarioFilePath = projectDirectory.resolve(IOUtils.SCENARIO_DIR).resolve(scenarioFile);

		if(!Files.exists(projectDirectory)) {
			logger.error("The file " + projectDirectory.toFile().toString() + " does not exist");
			System.exit(-1);
		}

		if(!Files.exists(scenarioFilePath)) {
			logger.error("The file " + scenarioFilePath.toFile().toString() + " does not exist");
			System.exit(-1);
		}


		logger.info(String.format("Running VADERE on %s...", scenarioFilePath));

		try {
			Scenario scenario = ScenarioFactory.createVadereWithProjectDirectory(
					projectDirectory.toFile().toString(),scenarioFile);
			new ScenarioRun(scenario, null).run();

		} catch (Exception e) {
			logger.error(e);
			System.exit(-1);
		}

	}
}
