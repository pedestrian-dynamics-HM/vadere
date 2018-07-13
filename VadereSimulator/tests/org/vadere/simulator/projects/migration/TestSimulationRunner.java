package org.vadere.simulator.projects.migration;

import org.junit.Test;
import org.vadere.simulator.control.ScenarioExecutorService;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TestSimulationRunner {

	private final String outputPath = "target/TestRuns/output";


	@Test
	public void RunAllSimulations() {
		// remove/clean dir from target folder and create new empty tree (used for output)
		try {
			Files.walk(Paths.get(outputPath).getParent())
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::deleteOnExit);
			Files.createDirectories(Paths.get(outputPath));
		} catch (IOException e) {
			e.printStackTrace();
		}

		ThreadFactory threadFactory = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				// System.out.println("creating pooled thread");
				final Thread thread = new Thread(r);
				//todo LogManager neuer Appender
//				thread.setUncaughtExceptionHandler(exceptionHandler);
				return thread;
			}
		};
		ExecutorService threadPool = ScenarioExecutorService.newFixedThreadPool(4, threadFactory);


		List<Path> scenarios = getScenarioFiles(Paths.get("../VadereModelTests/TestOSM"));
		List<Path> subset = scenarios.subList(0, 3);
		System.out.println(subset.size());
		for (Path scenarioPath : subset) {
			Scenario s = null;
			try {
				s = ScenarioFactory.createScenarioWithScenarioFilePath(scenarioPath);
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("#####Start scenario: " + scenarioPath.getFileName());
			threadPool.submit(new ScenarioRun(s, outputPath, (listener) -> System.out.println("done")));
		}

		threadPool.shutdown();
		if (!threadPool.isTerminated()) {
			try {
				System.out.println("waiting 60 sec....");
				threadPool.awaitTermination(60, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				threadPool.shutdownNow();
			}

		}

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
