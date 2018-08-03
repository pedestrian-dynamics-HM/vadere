package org.vadere.util.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * go recursively through the directory tree and copy alle files from src to dest.
 */
public class RecursiveCopy implements FileVisitor<Path> {

	private final Path src;
	private final Path dest;

	public RecursiveCopy(String src, String dest){
		this(Paths.get(src), Paths.get(dest));
	}

	public RecursiveCopy(Path src, Path dest){
		this.src = src;
		this.dest = dest;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Files.createDirectories(dest.resolve(src.relativize(dir)));
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Files.copy(file, dest.resolve(src.relativize(file)));
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}
}
