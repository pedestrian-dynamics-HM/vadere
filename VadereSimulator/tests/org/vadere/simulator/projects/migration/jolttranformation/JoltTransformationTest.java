package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.vadere.simulator.entrypoints.Version;

import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Before;
import org.vadere.simulator.projects.io.TestUtils;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertThat;


abstract class JoltTransformationTest {

	abstract protected  Path getTestDir();


	protected Path getTestDirBackup(){
		Path testDir = getTestDir();
		return testDir.getParent().resolve(testDir.getFileName() + ".bak");
	}

	protected Path getDirFromResources(String resource){
		URL resource1 = getClass().getResource(resource);
		File f = new File(resource1.getFile());
		return Paths.get(f.toString());
	}

	protected Path getTestFile(String fileName){
		return getTestDir().resolve(fileName);
	}

	protected Path getTestFileBackup(String fileName){
		return getTestDirBackup().resolve(fileName);
	}

	@Before
	public void init(){
		Path testDir = getTestDir();
		if (testDir != null) {
			Path testDirBackup = getTestDirBackup();
			TestUtils.copyDirTo(testDir, testDirBackup);
		}
	}

	@After
	public void cleaUp(){
		Path testDir = getTestDir();
		if (testDir != null) {
			Path testDirBackup = getTestDirBackup();
			TestUtils.copyDirTo(testDirBackup, testDir);
		}
	}


	/**
	 * Get path from JsonNode and assert that it exists.
	 */
	protected JsonNode pathMustExist(JsonNode root, String path){
		String[] pathElements = path.split("/");
		JsonNode ret = root;
		for (String item : pathElements) {
			ret = ret.path(item);
		}
		assertThat(ret, notMissing(path));
		return ret;
	}

	JsonNode getJson(String resources) throws IOException, URISyntaxException {
		URL url = getClass().getResource(resources);
		String json = IOUtils.readTextFile(Paths.get(url.toURI()));
		return StateJsonConverter.deserializeToNode(json);
	}

	StringContains versionMatcher(Version v){
		String pattern = "\"release\" : \"" + v.label('_') +"\"";
		return  new StringContains(pattern);
	}

	public Matcher<JsonNode> nodeHasText(String text){
		return new BaseMatcher<JsonNode>() {
			@Override
			public boolean matches(Object o) {
				final JsonNode n = (JsonNode)o;
				return n.asText().equals(text);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("<\"" + text + "\">");
			}
		};
	}

	public Matcher<JsonNode> notMissing(String path){
		return new BaseMatcher<JsonNode>() {
			@Override
			public boolean matches(Object o) {
				final JsonNode n = (JsonNode)o;
				return !n.isMissingNode();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("The nodes should not be empty. It should be reachable with Path: <" + path + ">" );
			}
		};
	}
}