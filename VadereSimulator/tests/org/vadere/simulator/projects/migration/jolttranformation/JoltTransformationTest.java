package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.*;

class JoltTransformationTest {


	JsonNode getJson(String resources) throws IOException, URISyntaxException {
		URL url = getClass().getResource(resources);
		String json = IOUtils.readTextFile(Paths.get(url.toURI()));
		return StateJsonConverter.deserializeToNode(json);
	}
}