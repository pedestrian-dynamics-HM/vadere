package org.vadere.simulator.projects.migration;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.Diffy;
import com.bazaarvoice.jolt.JsonUtils;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class JoltIdentityTransformationTest {


	@Test
	@Ignore
	public void testIdenityTransformationV02() throws IOException {
		List<Path> scenarioFiles = getScenarioFiles(
				Paths.get("../Scenarios/ModelTests").toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath());
		testIdentity(scenarioFiles, "/identity_v0.2.json");
	}

	// no TestResource in current git
	@Test
	@Ignore
	public void testIdenityTransformationV01() throws IOException {

		List<Path> scenarioFiles = getScenarioFiles(Paths.get("../Scenarios/ModelTestsV0.1").toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath());
		testIdentity(scenarioFiles, "/identity_v0.1.json");
	}

	@Test
	@Ignore
	public void testTransformationV01_to_V02() throws IOException {
		List<Path> scenarioFiles = getScenarioFiles(Paths.get("../Scenarios/ModelTestsV0.1").toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath());
		Diffy diffy = new Diffy();
		for (Path p : scenarioFiles) {
			Object jsonInput = JsonUtils.filepathToObject(p.toString());
			Chainr transformation =
					Chainr.fromSpec(JsonUtils.classpathToList("/transform_v0.1_to_v0.2.json"));
			Chainr identityV02 =
					Chainr.fromSpec(JsonUtils.classpathToList("/identity_v0.2.json"));
			Object jsonNew = transformation.transform(jsonInput);

			//test
			Object jsonNew2 = identityV02.transform(jsonNew);
			System.out.println(diffy.diff(jsonNew, jsonNew2).toString());
		}
	}

	@Test
	@Ignore
	public void migrationTest() throws IOException {
		Path testFile = Paths.get(getClass().getResource("/scenario_test.json").getPath());
		MigrationAssistant migrationAssistant = MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
//	    migrationAssistant.analyzeSingleScenario(testFile.toFile().toString());
	}


	private void testIdentity(List<Path> scenarioFiles, String transformation) {
		for (Path scenarioFile : scenarioFiles) {
			List chainrSpecJson = JsonUtils.classpathToList(transformation);
			Chainr chainr = Chainr.fromSpec(chainrSpecJson);

			Object inputJson = JsonUtils.filepathToObject(scenarioFile.toString());

			Object transformedOutput = chainr.transform(inputJson);

			Diffy diffy = new Diffy();
			Diffy.Result res = diffy.diff(inputJson, transformedOutput);
			System.out.printf("###Json Identity Transformation on Scenario: %s | Transformation Match: %s%n",
					scenarioFile.getFileName().toString(), res.isEmpty());
			if (!res.isEmpty()) {
				System.out.println(res.toString());
				System.out.println("######");
			}
			assertTrue(res.isEmpty());

		}
	}


	@Test
	@Ignore
	public void transformv1t0v2() throws IOException {
		Path scenario = Paths.get("../Scenarios/ModelTestsV0.1/TestOSM/scenarios/basic_1_chicken_osm1.scenario");
		List chainrSpecJson = JsonUtils.classpathToList("/transform_v0.1_to_v0.2.json");
		Chainr transform_v1_v2 = Chainr.fromSpec(chainrSpecJson);
		Object inputJson = JsonUtils.filepathToObject(scenario.toString());
		Object jsonOut1 = transform_v1_v2.transform(inputJson);
		Chainr identity_v2 = Chainr.fromSpec(JsonUtils.classpathToList("/identity_v0.2.json"));
		Object jsonOut2 = identity_v2.transform(jsonOut1);
		System.out.println(JsonUtils.toPrettyJsonString(jsonOut2));
		Diffy diffy = new Diffy();
		System.out.println(diffy.diff(jsonOut1, jsonOut2).toString());
	}

	@Test
	@Ignore
	public void attr01() throws IOException {
		List<Path> scenarioFiles = getScenarioFiles(Paths.get("../Scenarios/ModelTestsV0.1").toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath());
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Path scenarioFile : scenarioFiles) {
			Object inputJson = JsonUtils.filepathToObject(scenarioFile.toString());
			JsonObj va = get(new JsonObj(inputJson), "vadere", "topography");
//			System.out.println(JsonUtils.toPrettyJsonString(va.node));
			va.getMap().keySet().forEach(k -> out.put(k, va.getMap().get(k)));
		}
		System.out.println(JsonUtils.toPrettyJsonString(out));
	}

	private JsonObj get(JsonObj node, String... path) {

		if ((node.type == MAP) && path.length == 1) {
			LinkedHashMap<String, Object> m = node.getMap();
			Object o = m.get(path[0]);
			return new JsonObj(o);
		}
		if ((node.type == ARRAY) && path.length == 1) {
			return new JsonObj(node.getArray());
		}

		if (node.type == MAP && path.length > 1) {
			String next = path[0];
			String[] path2 = Arrays.copyOfRange(path, 1, path.length);
			return get(new JsonObj(node.getMap().get(next)), path2);
		}
		if (node.type == ARRAY) {
			throw new RuntimeException("only last element of pathMustExist can be an array");
		}
		return new JsonObj(node);
	}

	private static final int ARRAY = 1;
	private static final int MAP = 0;

	private class JsonObj {
		public Object node;
		public final int type;

		public JsonObj(Object node) {
			this.node = node;
			this.type = (node instanceof Map) ? MAP : ARRAY;
		}

		public LinkedHashMap<String, Object> getMap() {
			return (LinkedHashMap<String, Object>) node;
		}

		public ArrayList<Object> getArray() {
			return (ArrayList<Object>) node;
		}
	}


	private List<Path> getScenarioFiles(Path vadereModelTest) {
		LinkedList<Path> scenarioFiles = new LinkedList<>();
		FileVisitor<Path> visitor = new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				if (dir.endsWith("output")) {
					return FileVisitResult.SKIP_SUBTREE;
				} else {
					return FileVisitResult.CONTINUE;
				}
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (file.getFileName().toString().endsWith("scenario")) {
					scenarioFiles.add(file);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				return null;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
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