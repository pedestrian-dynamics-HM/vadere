package org.vadere.simulator.projects.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonElement;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HashGenerator {

	private static Logger logger = LogManager.getLogger(HashGenerator.class);

	//private static final String PATH_TO_COMMIT_HASH = System.getProperty("user.dir") + "/version-control/current_commit_hash.txt";

	public static String topographyHash(Topography topography) {
		String json = null;
		try {
			json = topography2Json(topography);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		String hash = DigestUtils.sha1Hex(json);
		return hash;
	}

	public static String attributesHash(final ScenarioStore store) {
		String json = attributes2Json(store);
		String hash = DigestUtils.sha1Hex(json);
		return hash;
	}

	public static String commitHash() {
		Scanner scanner = new Scanner(HashGenerator.class.getResourceAsStream("/current_commit_hash.txt"));
		String commithash = scanner.next();
		scanner.close();
		return commithash;
	}

	public static String releaseNumber() {
		Scanner scanner = new Scanner(HashGenerator.class.getResourceAsStream("/current_release_number.txt"));
		String releaseNumber = scanner.next();
		scanner.close();
		return releaseNumber;
	}

	private static String topography2Json(final Topography topography) throws JsonProcessingException {
		Topography topographyClone = topography.clone();
		topographyClone.removeBoundary();
		return JsonConverter.serializeTopography(topographyClone);
	}

	@Deprecated
	private static String attributes2Json(ScenarioStore attributes) {
		Map<String, Object> attributesMap = new HashMap<>();
		attributesMap.put("attributesModel", attributes.attributesList);
		attributesMap.put("attributesSimulation", attributes.attributesSimulation);
		// TODO [priority=high] [task=check] add more?
		String json = IOUtils.toPrettyPrintJson(attributesMap);
		return json;
	}
}
