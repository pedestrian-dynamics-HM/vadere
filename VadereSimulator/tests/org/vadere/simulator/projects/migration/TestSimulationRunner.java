package org.vadere.simulator.projects.migration;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.vadere.simulator.control.ScenarioExecutorService;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TestSimulationRunner {

	private static Logger logger = LogManager.getLogger(TestSimulationRunner.class);
	private final String outputPath = "target/TestRuns/output";

	private ThreadFactory threadFactory = r -> {
		final Thread thread = new Thread(r);
		thread.setUncaughtExceptionHandler( (t, e) -> {
			System.out.println(t.getName());

		});
		return thread;
	};

	private void initOutputDir(){
		try {
			if (Paths.get(outputPath).toFile().exists()) {
				Files.walk(Paths.get(outputPath).getParent())
						.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
			}
			Files.createDirectories(Paths.get(outputPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void RunAllSimulations() {
		// remove/clean dir from target folder and create new empty tree (used for output)
		initOutputDir();

		ExecutorService threadPool = ScenarioExecutorService.newFixedThreadPool(1, threadFactory);

		ScenarioAppender scenarioAppender = new ScenarioAppender(Paths.get(outputPath));
		LogManager.getRootLogger().addAppender(scenarioAppender);

//		StdOutRedirecter redirecter = new StdOutRedirecter();
//		redirecter.redirect();

		List<Path> scenarios = getScenarioFiles(Paths.get("../VadereModelTests"));
//		List<Path> scenarios = getScenarioFiles(Paths.get("../VadereModelTests/TestOSM"));
		List<Path> chicken = scenarios.stream().filter(f -> f.getFileName().endsWith("basic_1_chicken_osm1.scenario")).collect(Collectors.toList());
		List<Path> subset = scenarios.subList(0, 5);
		subset.addAll(chicken);
		System.out.println(subset.size());
		for (Path scenarioPath : subset) {
			Scenario s = null;
			try {
				s = ScenarioFactory.createScenarioWithScenarioFilePath(scenarioPath);
			} catch (IOException e) {
				e.printStackTrace();
			}

			logger.info("#####Start scenario: " + scenarioPath.getFileName());
			s.getAttributesSimulation().setWriteSimulationData(true);

			threadPool.submit(new ScenarioRun(s, outputPath, (listener) -> System.out.println("done")));
		}

		threadPool.shutdown();
		if (!threadPool.isTerminated()) {
			try {
				logger.info("waiting 10 min....");
				threadPool.awaitTermination(10, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				e.printStackTrace();
				threadPool.shutdownNow();
			} finally {
				LogManager.getRootLogger().removeAppender(scenarioAppender);
				System.out.println("before reset");
//				redirecter.reset();
				System.out.println("after reset");

			}

		}

	}

	private void addTrajectoriesOutputfile(final Scenario scenario){
		DataProcessingJsonManager m = scenario.getDataProcessingJsonManager();

	}

	@Test
	public void TestProjects(){
		List<Path> projects = getProjectDirs(Paths.get("../VadereModelTests/"));

		for (Path projectPath : projects) {
			//
		}

	}

	@Test
	public void getProjectDirsTest(){
		getProjectDirs(Paths.get("../VadereModelTests/")).forEach(p -> System.out.println(p.toString()));
	}

	private List<Path> getProjectDirs(Path vadereModelTest){
		LinkedList<Path> projectPath = new LinkedList<>();
		FileVisitor<Path> visitor = new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (Files.exists(dir.resolve("vadere.project"))){
					projectPath.add(dir);
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		};
		try {
			Files.walkFileTree(vadereModelTest, visitor);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return projectPath;
	}

	private List<Path> getScenarioFiles(Path vadereModelTest) {
		LinkedList<Path> scenarioFiles = new LinkedList<>();
		FileVisitor<Path> visitor = new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.endsWith("output")) {
					return FileVisitResult.SKIP_SUBTREE;
				} else {
					return FileVisitResult.CONTINUE;
				}
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.getFileName().toString().endsWith("scenario")) {
					scenarioFiles.add(file);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return null;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (Files.exists(dir.resolve(".git"))) {
					return FileVisitResult.CONTINUE;
				} else {
					return FileVisitResult.SKIP_SUBTREE;
				}
			}
		};
		try {
			Files.walkFileTree(vadereModelTest, visitor);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return scenarioFiles;
	}

}
