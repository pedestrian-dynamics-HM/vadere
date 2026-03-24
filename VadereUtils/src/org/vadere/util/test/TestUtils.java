package org.vadere.util.test;

import org.vadere.util.io.RecursiveCopy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

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

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static<T> T assertPresent(Optional<T> optional){
		if(optional.isEmpty()){
			throw new AssertionError("Optional is empty");
		}
		return optional.get();
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static<T> T assertPresent(Optional<T> optional, String errorMessage) {
		if(optional.isEmpty()){
			throw new AssertionError(errorMessage);
		}
		return optional.get();
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static<T> void assertMissing(Optional<T> optional){
		if(optional.isPresent()){
			throw new AssertionError("Optional should be empty but is present: " + optional.get());
		}
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static<T> void assertMissing(Optional<T> optional, String errorMessage) {
		if(optional.isPresent()){
			throw new AssertionError(errorMessage+": " + optional.get());
		}
	}
}
