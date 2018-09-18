package org.vadere.simulator.io;

import org.junit.Test;
import org.vadere.simulator.projects.io.HashGenerator;
import static org.junit.Assert.assertTrue;

public class TestHashGenerator {

	@Test
	public void testIsCommitHashFileInstalled() {
		assertTrue("missing commit hash file", HashGenerator.isCommitHashAvailable());
	}
}
