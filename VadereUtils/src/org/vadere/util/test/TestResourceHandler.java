package org.vadere.util.test;

import org.vadere.util.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

/**
 *  Manage resource directory used within a unit test. Implement {@link #getTestDir()} method
 *  and use default implementation to access resources.
 *
 * If a test changes files during execution it is useful to call {@link #backupTestDir()} in the
 * (at)Before method and {@link #loadFromBackup()} in the (at)After method to ensure a clean
 * working environment for each test even if previous test failed.
 */
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

	default Path getPathFromResources(String resource) {
		URL resource1 = getClass().getResource(resource);
		if (resource1 == null){
			fail("Resource not found: " + resource);
		}
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
