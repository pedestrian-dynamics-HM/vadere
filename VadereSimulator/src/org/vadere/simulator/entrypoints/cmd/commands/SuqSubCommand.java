package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.entrypoints.cmd.SubCommandRunner;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.control.simulation.ScenarioRun;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.util.logging.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SuqSubCommand implements SubCommandRunner {
	private final static Logger logger = Logger.getLogger(SuqSubCommand.class);

	@Override
	public void run(Namespace ns, ArgumentParser parser) {
		Path outputDir = Paths.get(ns.getString("output-dir"));
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
			ScenarioCache cache = ScenarioCache.load(scenario, scenarioFile.toAbsolutePath().getParent());
			new ScenarioRun(scenario, outputDir.toFile().toString(), true, null, scenarioFile, cache).run();
		} catch (Throwable e){
			e.printStackTrace();
			logger.error(e);
			System.exit(-1);
		}

	}
}
