package org.vadere.simulator.io;

import org.junit.Test;
import org.vadere.simulator.projects.io.HashGenerator;
import static org.junit.Assert.assertTrue;

public class TestHashGenerator {

	@Test
	public void testIsCommitHashFileInstalled() {
		assertTrue(
				"Missing or empty commit hash file. You can install a git hook to auto-create this file. See Documentation/version-control/.",
				HashGenerator.isCommitHashAvailable());
	}
}
