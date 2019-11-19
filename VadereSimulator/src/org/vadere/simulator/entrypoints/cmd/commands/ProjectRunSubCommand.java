package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.entrypoints.cmd.SubCommandRunner;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.control.simulation.ScenarioRun;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProjectRunSubCommand implements SubCommandRunner {
	private final static Logger logger = Logger.getLogger(ProjectRunSubCommand.class);


	@Override
	public void run(Namespace ns, ArgumentParser parser) throws Exception {
		Locale.setDefault(Locale.ENGLISH);
		String projectPath = ns.getString("project-dir");

		Path projectDirectory = Paths.get(projectPath);
		if(!Files.exists(projectDirectory.resolve(IOUtils.VADERE_PROJECT_FILENAME))) {
			logger.error("Direcotry is not a vadere project. " + projectPath);
			System.exit(-1);
		}

		if(!Files.isDirectory(projectDirectory.resolve(IOUtils.OUTPUT_DIR))){
			try {
				logger.info("create output folder.");
				Files.createDirectories(projectDirectory.resolve(IOUtils.OUTPUT_DIR));
			} catch (IOException e) {
				throw new IOException("cannot create output folder.", e);
			}
		}

		logger.info("collect scenario files.");
		List<String> scenarioFileNames;
		Path scenarioFilePath = projectDirectory.resolve(IOUtils.SCENARIO_DIR);
		if(Files.isDirectory(scenarioFilePath)){
			scenarioFileNames = Files.walk(scenarioFilePath)
					.filter(f -> Files.isRegularFile(f) && f.toString().endsWith(IOUtils.SCENARIO_FILE_EXTENSION))
					.map(f -> f.getFileName().toString())
					.collect(Collectors.toList());
		} else {
			throw new IOException("scenario folder not found in project. " + projectDirectory);
		}

		if (scenarioFileNames.size() > 0){
			logger.info("found " + scenarioFileNames.size() + " scenarios ...");
		} else {
			throw new IOException("no scenarios found in project");
		}

		int i = 0;
		for (String scenarioFileName : scenarioFileNames) {
			i++;
			logger.info(String.format("%d/%d Running VADERE on %s...",i, scenarioFileNames.size(), scenarioFileName));

			try {
				Scenario scenario = ScenarioFactory.createVadereWithProjectDirectory(
						projectDirectory.toFile().toString(),scenarioFileName);
				ScenarioCache cache = ScenarioCache.load(scenario, scenarioFilePath.toAbsolutePath().getParent());
				new ScenarioRun(scenario, projectDirectory.resolve(IOUtils.OUTPUT_DIR).toString(), null, projectDirectory.resolve(IOUtils.OUTPUT_DIR), cache).run();

			} catch (Throwable e) {
				logger.error(String.format("Error while executing scenario %d/%d %s. Resume with next scenario.", i, scenarioFileNames.size(), scenarioFileName));
				System.exit(-1);
			}
		}




	}
}
