package org.vadere.simulator.projects.io;

import org.vadere.util.io.RecursiveCopy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class TestUtils {

	public static void resetTestStructure(String dest, String backup) throws URISyntaxException {
		try {

			if (Paths.get(dest).toFile().exists()) {
				Files.walk(Paths.get(dest))
						.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
			}
			Files.walkFileTree(Paths.get(backup), new RecursiveCopy(backup, dest));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
