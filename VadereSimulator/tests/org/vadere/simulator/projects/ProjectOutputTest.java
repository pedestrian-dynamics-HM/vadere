package org.vadere.simulator.projects;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.simulator.projects.io.IOOutput;
import org.vadere.simulator.projects.io.IOVadere;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectOutputTest {

	private VadereProject project;
	private ProjectOutput projectOutput;

	@Before
	public void setup() throws URISyntaxException, IOException {
		Path projectPath = Paths.get(getClass().getResource("/data/simpleProject").toURI());
		project = IOVadere.readProject(projectPath.toString());
		projectOutput = new ProjectOutput(project);
	}

	@After
	public void cleanup() throws URISyntaxException, IOException {
		if (!Files.exists(project.getOutputDir().resolve("testOutput2"))) {
			Path backup = Paths.get(getClass().getResource("/data/testOutput2").toURI());
			FileUtils.copyDirectoryToDirectory(backup.toFile(), project.getOutputDir().toFile());
			IOOutput.deleteOutputDirectory(project.getOutputDir().resolve("corrupt/testOutput2").toFile());
		}
	}

	@Test
	public void getAllOutputDirs() throws Exception {
		List<File> out = projectOutput.getAllOutputDirs();
		assertEquals("There should be 14 output directories", 14, out.size());
	}

	@Test
	public void markDirty() {
		projectOutput.markDirty("testOutput2");
		Optional<SimulationOutput> out = projectOutput.getSimulationOutput("testOutput2");
		assertTrue(out.isPresent());
		assertTrue(out.get().isDirty());
	}

	@Test
	public void cleanOutputDirs() throws IOException {
		projectOutput.markDirty("testOutput2");
		Files.delete(project.getOutputDir().resolve(Paths.get("testOutput2", "test_postvis.scenario")));
		projectOutput.cleanOutputDirs();
		assertFalse(projectOutput.getSimulationOutput("testOutput2").isPresent());
		assertFalse(Files.exists(project.getOutputDir().resolve("testOutput2")));
	}

	@Test
	@Ignore
	public void readTest() throws IOException {

//		for(int i = 0 ; i < 3; i++) {
//			long startTime = System.nanoTime();
//			String text1 = IOUtils.readTextFile(simOutDir.resolve("postvis.trajectories"));
//			long endTime = System.nanoTime();
//			long duration = (endTime - startTime);
//			System.out.format("(postvis.trajectories)  readTextFile:  %10d%n", duration);
//
//			startTime = System.nanoTime();
//			String text2 = IOUtils.readTextFile2(simOutDir.resolve("postvis.trajectories"));
//			endTime = System.nanoTime();
//			duration = (endTime - startTime);
//			System.out.format("(postvis.trajectories)  readTextFile2: %10d%n", duration);
//
//			startTime = System.nanoTime();
//			String text3 = IOUtils.readTextFile(simOutDir.resolve("test_postvis.scenario"));
//			endTime = System.nanoTime();
//			duration = (endTime - startTime);
//			System.out.format("(test_postvis.scenario) readTextFile3: %10d%n", duration);
//
//			startTime = System.nanoTime();
//			String text4 = IOUtils.readTextFile2(simOutDir.resolve("test_postvis.scenario"));
//			endTime = System.nanoTime();
//			duration = (endTime - startTime);
//			System.out.format("(test_postvis.scenario) readTextFile4: %10d%n", duration);
//			System.out.println("--------------------------------------------------------");
//
//			assertEquals(text1,text2);
//			assertEquals(text3,text4);
//		}


	}
}
