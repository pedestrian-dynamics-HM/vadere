package org.vadere.simulator.utils.reflection;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

/**
 * Helper Methods for dealing with JsonNodes in TestFiles.
 */
public interface TestJsonNodeHelper {

	/**
	 * Get path from JsonNode and assert that it exists.
	 */

	default JsonNode getJsonFromString(String s) {
		JsonNode ret = null;
		try {
			ret = StateJsonConverter.deserializeToNode(s);
		} catch (IOException e) {
			fail("Cannot create Json object from string: " + s);
		}
		return ret;
	}

	default JsonNode getJsonFromPath(Path path) {
		JsonNode ret = null;
		String json;
		try {
			json = IOUtils.readTextFile(path);
			ret = StateJsonConverter.deserializeToNode(json);
		} catch (Exception e) {
			fail("Cannot create Json object from path: " + path.toString());
		}
		return ret;
	}

	default JsonNode getJsonFromResource(String resources) {
		URL url = getClass().getResource(resources);
		String json;
		JsonNode ret = null;
		try {
			json = IOUtils.readTextFile(Paths.get(url.toURI()));
			ret = StateJsonConverter.deserializeToNode(json);
		} catch (Exception e) {
			fail("Cannot create Json object from resource: " + resources);
		}
		return ret;
	}

}
