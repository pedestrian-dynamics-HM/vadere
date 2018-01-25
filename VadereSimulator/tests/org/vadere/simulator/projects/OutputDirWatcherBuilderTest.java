package org.vadere.simulator.projects;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.util.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Stefan Schuhbäck
 */
public class OutputDirWatcherBuilderTest {

	private VadereProject project;
	private OutputDirWatcherBuilder builder;


	@Before
	public void setup() throws URISyntaxException, IOException {
		Path projectPath = Paths.get(getClass().getResource("/data/simpleProject").toURI());
		project = IOVadere.readProject(projectPath.toString());
		builder = new OutputDirWatcherBuilder();
	}

	@Test
	public void initOutputDirWatcher() throws Exception {
		builder.initOutputDirWatcher(project);
		assertNotNull(builder.getWatchService());
		assertNotNull(builder.getKeys());
		assertNotNull(builder.getHandlers());
	}

	@Test
	public void registerDefaultDir() throws Exception {
		builder.initOutputDirWatcher(project).registerDefaultDir();
		assertEquals("There should be three default Handlers",
				5,builder.getHandlers().size());
		assertEquals("There should be 15 directories in the watch list",
				14, builder.getKeys().size());

		Path corruptedDir = project.getOutputDir().resolve(IOUtils.CORRUPT_DIR);
		builder.getKeys().forEach((k,v) -> {
			assertNotEquals("The corrupt directory should not be watched",
					corruptedDir, v);
		});

	}

	@Test
	public void build() throws Exception {
	}

	@Test
	public void addEventHandler() throws Exception {
		WatchEventHandler handler = (dir, ev) -> {
			System.out.print("XXX");
		};
		builder.initOutputDirWatcher(project).addEventHandler(handler);
		assertEquals("There should be one Handler",
				1,
				builder.getHandlers().size());

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		System.setOut(new PrintStream(buffer));

		builder.getHandlers().forEach( h -> h.processEvent(null, null));

		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
		assertEquals("WatchEventHandler output wrong", "XXX", buffer.toString());

	}

	@Test
	public void register() throws Exception {
		builder.initOutputDirWatcher(project)
				.register(project.getOutputDir().resolve("test_postvis_2018-01-17_16-57-06.272"));
		assertEquals("There should be one watched directory", 1,builder.getKeys().size());

		builder.register(project.getOutputDir().resolve("corrupt/test_postvis_2018-01-19_13-38-01.695"));
		assertEquals("There should be one watched directory", 1,builder.getKeys().size());
	}

	@Test
	public void registerAll() throws Exception {
		builder.initOutputDirWatcher(project).registerAll(project.getOutputDir());
		assertEquals("There should be 15 directories in the watch list",
				14, builder.getKeys().size());
	}

}