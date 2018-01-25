package org.vadere.simulator.projects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.simulator.projects.io.IOVadere;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author  Stefan Schuhbäck
 */
public class OutputDirWatcherTest {

	private VadereProject project;

	@Before
	public void setup() throws URISyntaxException, IOException {
		Path projectPath = Paths.get(getClass().getResource("/data/simpleProject").toURI());
		project = IOVadere.readProject(projectPath.toString());
	}

	@Test @Ignore
	public void OutputDirWatcherEventTest() throws IOException, InterruptedException {

		OutputDirWatcherBuilder builder = new OutputDirWatcherBuilder();
		builder.initOutputDirWatcher(project);
		builder.registerDefaultDir();
		OutputDirWatcher dirWatcher = builder.build();

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<?> future = executor.submit(dirWatcher);
		executor.shutdown();

		// Now, the watcher runs in parallel
		// Do other stuff here

		// Shutdown after 10 seconds
		executor.awaitTermination(30, TimeUnit.SECONDS);

		// abort watcher
		future.cancel(true);

		executor.awaitTermination(1, TimeUnit.SECONDS);

		executor.shutdownNow();
	}

	private void addDummyEventHandler(OutputDirWatcherBuilder builder) {

	}

}
