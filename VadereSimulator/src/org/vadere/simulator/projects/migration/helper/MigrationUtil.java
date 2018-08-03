package org.vadere.simulator.projects.migration.helper;

import org.apache.log4j.Logger;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.MigrationOptions;
import org.vadere.simulator.projects.migration.jolttranformation.JoltTransformation;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MigrationUtil {

	private final static Logger logger = Logger.getLogger(MigrationUtil.class);

	public static void main(String[] args) throws URISyntaxException, IOException {
		MigrationUtil migrationUtil = new MigrationUtil();


//		migrationUtil.migrateTestData(Paths.get("/home/lphex/hm.d/vadere/"));
		migrationUtil.generateNewVersionTransform(Paths.get("/home/lphex/hm.d/vadere/VadereSimulator/resources"), "0.4");
	}


	private ArrayList<String> createList(String... addAll){
		return Arrays.stream(addAll).collect(Collectors.toCollection(ArrayList::new));
	}

	private void generateNewVersionTransform(Path resourceDir, String newVersionLabel) throws URISyntaxException, IOException {

		Path oldTransform = JoltTransformation.getTransforamtionFile(Version.latest());
		Path oldIdentity = JoltTransformation.getIdenityFile(Version.latest());

		String newTransformString = JoltTransformation
				.getTransforamtionResourcePath(Version.latest().label('-'), newVersionLabel);
		String newIdenityString = JoltTransformation
				.getIdentiyResoucrePath(newVersionLabel);

		Path newTransform = resourceDir.resolve(newTransformString.substring(1));
		Path newIdenity = resourceDir.resolve(newIdenityString.substring(1));

		String json = IOUtils.readTextFile(oldIdentity);
		json =  json.replace("\"release\": \"" + Version.latest().label('-') + "\",", "\"release\": \"" + newVersionLabel + "\",");
		IOUtils.writeTextFile(newIdenity.toString(), json);

		json =  json.replace("\"release\": \"&\",", "// no relase here to overwrite it with default at the default operation down below");
		IOUtils.writeTextFile(newTransform.toString(), json);

	}

	/**
	 *
	 * @param p root path of repo
	 */
	private void migrateTestData(Path p) {
		ArrayList<String> ignoreDirs = createList("VadereModelTests", "target", "Documentation");
		ArrayList<String> dirMarker = createList("DO_NOT_MIGRATE", ".DO_NOT_MIGRATE");
		ArrayList<String> treeMarker =  createList("DO_NOT_MIGRATE_TREE", ".DO_NOT_MIGRATE_TREE");
		FileVisitor<Path> visitor = getVisitor(ignoreDirs, treeMarker, dirMarker, this::migrateToLatestVersion);

		try {
			Files.walkFileTree(p, visitor);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean migrateToLatestVersion(Path path){
		MigrationAssistant ms = MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
		try {
			ms.migrateFile(path, Version.latest(), null);
			return true;
		} catch (MigrationException e) {
			logger.error("Error in MigrationUtil stop: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private boolean revert(Path path){
		MigrationAssistant ms = MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
		try{
			ms.revertFile(path);
			return true;
		} catch (MigrationException e){
			logger.error("Error in MigraionUtil: " + e.getMessage());
			return false;
		}
	}

	FileVisitor<Path> getVisitor(ArrayList<String> ignoreDirs, ArrayList<String> treeMarker, ArrayList<String> dirMarker, Function<Path, Boolean> func) {
		return  new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				boolean hasTreeMarker = treeMarker.stream().anyMatch( m -> dir.resolve(m).toFile().exists());
				if (ignoreDirs.contains(dir.getFileName().toString())){ // skip ignored directories-trees.
					return FileVisitResult.SKIP_SUBTREE;
				} else if (hasTreeMarker) { // skip directory tree if treeMarker is present.
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

				// continue traversal if not scenario
				if (! file.getFileName().toString().endsWith("scenario"))
					return FileVisitResult.CONTINUE;

				// if dirMarker is set do not migrate any scenario files in this dir
				// but continue with sub-dirs.
				boolean hasDirMarker = dirMarker.stream().anyMatch(m -> file.getParent().resolve(m).toFile().exists());
				if (hasDirMarker){
					return FileVisitResult.CONTINUE;
				}

				boolean ret = func.apply(file);
				if (ret){
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
