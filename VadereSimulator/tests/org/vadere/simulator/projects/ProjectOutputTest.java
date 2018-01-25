package org.vadere.simulator.projects;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.util.io.IOUtils;
import static org.junit.Assert.*;

public class ProjectOutputTest {

	private Path simOutDir;

	@Before
	public void setup() throws URISyntaxException {
		simOutDir = Paths.get(getClass().
				getResource("/data/simpleProject/output/test_postvis_2018-01-17_16-56-37.307").toURI());
	}



	@Test @Ignore
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
