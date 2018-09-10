package org.vadere.tests.util.reflection;

import com.fasterxml.jackson.databind.JsonNode;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jolttranformation.JsonNodeExplorer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringJoiner;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


/**
 * Overwrite {@link JsonNodeExplorer} function to allow Hamcrest Matchers for Tests.
 */
public interface TestJsonNodeExplorer extends JsonNodeExplorer {
	default JsonNode path(JsonNode root, String path) {
		String[] pathElements = path.split("/");
		JsonNode ret = root;
		for (String item : pathElements) {
			ret = ret.path(item);
		}
		return ret;
	}

	default JsonNode pathMustExist(JsonNode root, String path) {
		JsonNode ret = path(root, path);
		assertThat(ret, notMissing(path));
		return ret;
	}

	default StringContains versionMatcher(Version v) {
		String pattern = "\"release\" : \"" + v.label('_') + "\"";
		return new StringContains(pattern);
	}

	default Matcher<JsonNode> nodeHasText(String text) {
		return new BaseMatcher<JsonNode>() {
			@Override
			public boolean matches(Object o) {
				final JsonNode n = (JsonNode) o;
				return n.asText().equals(text);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("<\"" + text + "\">");
			}
		};
	}

	default Matcher<JsonNode> notMissing(String path) {
		return new BaseMatcher<JsonNode>() {
			@Override
			public boolean matches(Object o) {
				final JsonNode n = (JsonNode) o;
				return !n.isMissingNode();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("The nodes should not be empty. It should be reachable with Path: <" + path + ">");
			}
		};
	}

	default void assertLatestReleaseVersion(JsonNode root) {
		assertThat("Version must be latest:  + Version.latest().toString()",
				pathMustExist(root, "release"),
				nodeHasText(Version.latest().label('-')));
	}


	default void assertReleaseVersion(JsonNode root, Version v, String reason) {
		assertThat("Old Version must be 0.1",
				pathMustExist(root, "release"),
				nodeHasText(v.label('-')));
	}

	default JsonNode pathLastElementMustNotExist(JsonNode root, String path) {
		String[] pathElements = path.split("/");
		JsonNode ret = root;
		StringJoiner sj = new StringJoiner("/");
		for (int i = 0; i < pathElements.length; i++) {
			ret = ret.path(pathElements[i]);
			sj.add(pathElements[i]);
			if (i == pathElements.length - 1) {
				assertThat("The last element should be missing", ret, not(notMissing(sj.toString())));
			} else {
				assertThat("Path elements up to the last must be present.", ret, notMissing(sj.toString()));
			}
		}
		return ret;
	}

	default JsonNode pathMustNotExist(JsonNode root, String path) {
		JsonNode ret = path(root, path);
		assertThat(ret, not(notMissing(path)));
		return ret;
	}

	default ArrayList<JsonNode> getProcessorsByType(JsonNode node, String processorType)  {
		try {
			return JsonNodeExplorer.super.getProcessorsByType(node, processorType);
		} catch (MigrationException e) {
			fail("default nodes not present.");
			return null;
		}
	}

	default ArrayList<JsonNode> getFilesForProcessorId(JsonNode node, String processorType){
		try {
			return JsonNodeExplorer.super.getFilesForProcessorId(node, processorType);
		} catch (MigrationException e) {
			fail("default nodes not present.");
			return null;
		}
	}
}
