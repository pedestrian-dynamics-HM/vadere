package org.vadere.simulator.projects;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author  Stefan Schuhbäck
 */
public class OutputDirWaterTest {

	private Path outputDir;
	OutputDirWatcher dirWatcher;

	@Before
	public void setup() throws Exception {
		outputDir = Paths.get(getClass().getResource("/data/simpleProject/output").toURI());
//		dirWatcher = new OutputDirWatcher();
	}


	@Test @Ignore
	public void testist(){
		List<WatcherHandler> list = new ArrayList<>();
//		list.add(new WatcherHandlerImpl("Hi") {
//			@Override
//			public void doit() {
//				System.out.println(s);
//			}
//		});
//
//		list.add(new WatcherHandlerImpl("World!") {
//			@Override
//			public void doit() {
//				System.out.println(s);
//			}
//		});
//		String s = "hi";
//		WatcherHandler w = () -> System.out.println(s);
//
//				list.add(w);
//		list.add(WatcherHandler -> System.out.println(s.toUpperCase()));
//		list.forEach(e -> e.);

	}

	@Test @Ignore
	public void WatchTest() throws IOException, InterruptedException {
		dirWatcher.registerAll(outputDir);
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

	@Test @Ignore
	public void numberOfWatchedDirsTest() throws IOException {
		dirWatcher.registerAll(outputDir);
		ConcurrentHashMap<WatchKey, Path> map = dirWatcher.getKeys();
		assertEquals("There should be 14 simulation dirs and the output dir itself!",
				15 ,map.size());
	}

	@Test @Ignore
	public void ignoreCourruptedDirTest() throws IOException {
		dirWatcher.registerAll(outputDir);
		ConcurrentHashMap<WatchKey, Path> map = dirWatcher.getKeys();
		map.forEach((watchKey, path) -> {
			assertFalse("Directory corrupt should not be watched! ",path.endsWith("corrupt"));
		});
	}
}
