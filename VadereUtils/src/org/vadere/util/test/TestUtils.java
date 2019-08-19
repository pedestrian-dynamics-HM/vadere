package org.vadere.util.test;

import org.vadere.util.io.RecursiveCopy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class TestUtils {

	public static void copyDirTo(Path source, Path dest){
		try {

			if (dest.toFile().exists()) {
				Files.walk(dest)
						.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
			}
			Files.walkFileTree(source, new RecursiveCopy(source, dest));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void copyDirTo(String source, String dest) {
		copyDirTo(Paths.get(source), Paths.get(dest));
	}
}
