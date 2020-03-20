package org.vadere.simulator.utils.reflection;

import com.fasterxml.jackson.databind.JsonNode;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.jsontranformation.JsonNodeExplorer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.function.Predicate;

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

	default Matcher<JsonNode> missingNode() {
		return new BaseMatcher<JsonNode>() {
			@Override
			public boolean matches(Object o) {
				final JsonNode n = (JsonNode) o;
				return n.isMissingNode();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("The nodes should be empty.");
			}
		};
	}

	default Matcher<JsonNode> measurementAreaExists(int id){
		return new BaseMatcher<JsonNode>() {
			@Override
			public boolean matches(Object o) {
				final JsonNode n = (JsonNode) o;
				try {
					Iterator<JsonNode> iter = iteratorMeasurementArea(n,id);
					return iter.hasNext();
				} catch (Exception e) {
					fail(e.getMessage());
				}
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("No measurementAreaExists found with Id:" + id);
			}
		};
	}


	default Matcher<JsonNode> fieldChanged(String relPath, String oldName, String newName, Predicate<JsonNode> typeTest){
		return new BaseMatcher<JsonNode>() {

			String text;

			@Override
			public boolean matches(Object o) {
				JsonNode node = (JsonNode)o;
				JsonNode oldField = path(node, relPath + oldName);
				JsonNode newField = path(node, relPath + newName);
				if (!oldField.isMissingNode() || newField.isMissingNode()){
					text = String.format("Expected field with name %s, no with %s", newName, oldName);
				} else if (!typeTest.test(newField)){
					text = String.format("New field has wrong Type %s", newField.getNodeType().name());
				}
				return oldField.isMissingNode() && !newField.isMissingNode() && typeTest.test(newField);
			}

			@Override
			public void describeTo(Description description) {

				description.appendText(text);
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
		} catch (Exception e) {
			fail("default nodes not present.");
			return null;
		}
	}

	default ArrayList<JsonNode> getFilesForProcessorId(JsonNode node, String processorType){
		try {
			return JsonNodeExplorer.super.getFilesForProcessorId(node, processorType);
		} catch (Exception e) {
			fail("default nodes not present.");
			return null;
		}
	}
}
