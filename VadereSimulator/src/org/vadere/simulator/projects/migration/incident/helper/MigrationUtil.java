package org.vadere.simulator.projects.migration.incident.helper;

import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.MigrationOptions;
import org.vadere.simulator.projects.migration.jsontranformation.JoltTransformation;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class MigrationUtil {

	private final static Logger logger = Logger.getLogger(MigrationUtil.class);

	private ArrayList<String> dirMarker;
	private ArrayList<String> treeMarker;
	private ArrayList<String> ignoreDirs;
	Throwable err;

	public MigrationUtil() {
		dirMarker = createList("DO_NOT_MIGRATE", ".DO_NOT_MIGRATE");
		treeMarker = createList("DO_NOT_MIGRATE_TREE", ".DO_NOT_MIGRATE_TREE", IOUtils.CORRUPT_DIR, IOUtils.LEGACY_DIR);
		ignoreDirs = createList("Scenarios", "target", "Documentation");
	}

	public static void main(String[] args) throws URISyntaxException, IOException {
		MigrationUtil migrationUtil = new MigrationUtil();

//		migrationUtil.migrateTestData(Paths.get("/home/lphex/hm.d/vadere/"));
//		migrationUtil.generateNewVersionTransform(Paths.get("/home/lphex/hm.d/vadere/VadereSimulator/resources"), "0.4");
	}


	private ArrayList<String> createList(String... addAll) {
		return Arrays.stream(addAll).collect(Collectors.toCollection(ArrayList::new));
	}

	public void generateNewVersionTransform(Path resourceDir, String newVersionLabel) throws URISyntaxException, IOException {

		Path oldTransform = JoltTransformation.getTransformationFileFromFileSystem(resourceDir, Version.latest());
		Path oldIdentity = JoltTransformation.getIdenityFileFromFileSystem(resourceDir, Version.latest());

		String newTransformString = JoltTransformation
				.getTransforamtionResourcePath(Version.latest().label('-'), newVersionLabel);
		String newIdenityString = JoltTransformation
				.getIdentiyResoucrePath(newVersionLabel);

		Path newTransform = resourceDir.resolve(newTransformString.substring(1));
		Path newIdenity = resourceDir.resolve(newIdenityString.substring(1));

		String json = IOUtils.readTextFile(oldIdentity);
		json = json.replace("\"release\": \"" + Version.latest().label('-') + "\",", "\"release\": \"" + newVersionLabel + "\",");
		IOUtils.writeTextFile(newIdenity.toString(), json);

		json = json.replace("\"release\": \"&\",", "// no release here to overwrite it with default at the default operation down below");
		IOUtils.writeTextFile(newTransform.toString(), json);

	}

	public void migrateDirectoryTree(Path p, Version targetVersion, boolean recursive) throws MigrationException {
		FileVisitor<Path> visitor = getVisitor(new ArrayList<>(), treeMarker, dirMarker, recursive, path -> migrate(path, targetVersion));
		this.err = null;
		try {
			Files.walkFileTree(p, visitor);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (this.err != null) {
			throw new MigrationException("topographyError while processing MigrationTaskHandler in walkFileTree", this.err);
		}

	}

	public void revertDirectoryTree(Path p, boolean recursive) throws MigrationException {
		FileVisitor<Path> visitor = getVisitor(new ArrayList<>(), treeMarker, dirMarker, recursive, this::revert);
		this.err = null;
		try {
			Files.walkFileTree(p, visitor);
		} catch (IOException e) {
			throw new MigrationException("topographyError in walkFileTree", e);
		}
		if (this.err != null) {
			throw new MigrationException("topographyError while processing MigrationTaskHandler in walkFileTree", this.err);
		}
	}

	/**
	 * Called in walkFileTree call and thus cannot throw exception. save Throwable and aboard the
	 * FileTreeWalk the calling funtion will check if an topographyError occurred and will throw the Throwable
	 * upstream.
	 */
	private boolean migrate(Path path, Version version) {
		MigrationAssistant ms = MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
		try {
			logger.info("migrate: " + path.toString());
			ms.migrateScenarioFile(path, Version.latest(), null);
			return true;
		} catch (MigrationException e) {
			logger.error("Error in MigrationUtil stop: " + e.getMessage());
			this.err = e;
			return false;
		}
	}

	/**
	 * Called in walkFileTree call and thus cannot throw exception. save Throwable and aboard the
	 * FileTreeWalk the calling funtion will check if an topographyError occurred and will throw the Throwable
	 * upstream.
	 */
	private boolean revert(Path path) {
		MigrationAssistant ms = MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
		try {
			ms.revertFile(path);
			return true;
		} catch (MigrationException e) {
			logger.error("Error in MigraionUtil: " + e.getMessage());
			this.err = e;
			return false;
		}
	}

	FileVisitor<Path> getVisitor(ArrayList<String> ignoreDirs, ArrayList<String> treeMarker,
								 ArrayList<String> dirMarker, boolean recursive, MigrationTaskHandler func) {


		return new FileVisitor<Path>() {

			private int maxRecursion = 0; //

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

				boolean hasTreeMarker = treeMarker.stream().anyMatch(m -> dir.resolve(m).toFile().exists());

				if (ignoreDirs.contains(dir.getFileName().toString())) { // skip ignored directories-trees.
					return FileVisitResult.SKIP_SUBTREE;
				} else if (hasTreeMarker) { // skip directory tree if treeMarker is present.

					if (dir.resolve(IOUtils.LEGACY_DIR).toFile().exists()){
						throw new RuntimeException("Cannot migrate directory " + dir + " due to a legacy folder. Delete the legacy folder and re-run the migration.");
					}

					return FileVisitResult.SKIP_SUBTREE;
				}

				// if recursive is true then continue all the way down.
				if (recursive)
					return FileVisitResult.CONTINUE;


				// if recursive is false we must enter only in the first directory and then
				// skip all others found.
				if (maxRecursion > 1) {
					return FileVisitResult.SKIP_SUBTREE;
				} else {
					maxRecursion++;
					return FileVisitResult.CONTINUE;
				}

			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

				// continue traversal if not scenario
				if (!file.getFileName().toString().endsWith(".scenario"))
					return FileVisitResult.CONTINUE;

				// if dirMarker is set do not migrate any scenario files in this dir
				// but continue with sub-dirs.
				boolean hasDirMarker = dirMarker.stream().anyMatch(m -> file.getParent().resolve(m).toFile().exists());
				if (hasDirMarker) {
					return FileVisitResult.CONTINUE;
				}

				boolean ret = func.handle(file);
				if (ret) {
					return FileVisitResult.CONTINUE;
				} else {
					return FileVisitResult.TERMINATE;
				}

			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				return null;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
				return FileVisitResult.CONTINUE;
			}
		};

	}
}
