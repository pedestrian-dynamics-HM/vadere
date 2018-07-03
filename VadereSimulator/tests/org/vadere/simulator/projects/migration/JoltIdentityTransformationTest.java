package org.vadere.simulator.projects.migration;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.Diffy;
import com.bazaarvoice.jolt.JsonUtils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class JoltIdentityTransformationTest {


	@Test
	public void testExistingScenarioFiles() throws IOException {
		List<Path> scenarioFiles = getScenarioFiles(Paths.get("../VadereModelTests").toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath());

		for (Path scenarioFile : scenarioFiles) {
			List chainrSpecJson = JsonUtils.classpathToList("/transfrom_v2_to_v3.json");
			Chainr chainr = Chainr.fromSpec(chainrSpecJson);

			Object inputJson = JsonUtils.filepathToObject(scenarioFile.toString());

			Object transformedOutput = chainr.transform(inputJson);

			Diffy diffy = new Diffy();
			Diffy.Result res = diffy.diff(inputJson, transformedOutput);
			System.out.printf("###Test Jolt Identity on Scenario: %s | Transformation Match: %s%n",
					scenarioFile.getFileName().toString(), res.isEmpty());
			if (!res.isEmpty()) {
				System.out.println(res.toString());
				System.out.println("######");
				System.out.println(JsonUtils.toPrettyJsonString(transformedOutput));
			}

		}

	}

	private List<Path> getScenarioFiles(Path vadereModelTest) {
		LinkedList<Path> scenarioFiles = new LinkedList<>();
		FileVisitor<Path> visitor = new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.endsWith("output") || dir.endsWith("TestSFM")) {
					return FileVisitResult.SKIP_SUBTREE;
				} else {
					return FileVisitResult.CONTINUE;
				}
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.getFileName().toString().endsWith("scenario")) {
					scenarioFiles.add(file);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return null;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (Files.exists(dir.resolve(".git"))) {
					return FileVisitResult.CONTINUE;
				} else {
					return FileVisitResult.SKIP_SUBTREE;
				}
			}
		};
		try {
			Files.walkFileTree(vadereModelTest, visitor);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return scenarioFiles;
	}
}