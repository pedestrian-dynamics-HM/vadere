package org.vadere.tests.util.reflection;

import org.vadere.simulator.projects.io.TestUtils;
import org.vadere.util.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

public interface TestResourceHandler {

	Path getTestDir();

	default void backupTestDir(){
		Path testDir = getTestDir();
		if (testDir != null) {
			Path testDirBackup = getTestDirBackup();
			TestUtils.copyDirTo(testDir, testDirBackup);
		}
	}

	default void loadFromBackup() {
		Path testDir = getTestDir();
		if (testDir != null) {
			Path testDirBackup = getTestDirBackup();
			TestUtils.copyDirTo(testDirBackup, testDir);
		}
	}

	default Path getTestDirBackup() {
		Path testDir = getTestDir();
		return testDir.getParent().resolve(testDir.getFileName() + ".bak");
	}

	default Path getDirFromResources(String resource) {
		URL resource1 = getClass().getResource(resource);
		File f = new File(resource1.getFile());
		return Paths.get(f.toString());
	}

	default Path getRelativeTestPath(String fileName) {
		return getTestDir().resolve(fileName);
	}

	default String getTestFileAsString(String fileName) {
		String ret = null;
		try {
			ret = IOUtils.readTextFile(getRelativeTestPath(fileName));
		} catch (IOException e) {
			fail("File not Found: " + e.getMessage());
		}
		return ret;
	}

	default Path getTestFileBackup(String fileName) {
		return getTestDirBackup().resolve(fileName);
	}
}
