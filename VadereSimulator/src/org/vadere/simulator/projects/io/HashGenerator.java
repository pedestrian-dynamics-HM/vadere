package org.vadere.simulator.projects.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HashGenerator {

	private static Logger logger = LogManager.getLogger(HashGenerator.class);
	
	private static final String CURRENT_RELEASE_NUMBER_RESOURCE = "/current_release_number.txt";
	private static final String CURRENT_COMMIT_HASH_RESOURCE = "/current_commit_hash.txt";

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

	public static boolean isCommitHashAvailable() {
		return getFirstStringTokenFromResource(CURRENT_COMMIT_HASH_RESOURCE) != null;
	}

	public static String commitHash() {
		String commitHash = getFirstStringTokenFromResource(CURRENT_COMMIT_HASH_RESOURCE);

		if (commitHash == null) {
			commitHash = "warning: no commit hash";
			logger.warn("No commit hash found. The project will not contain a hash of the software source code.");
		}

		return commitHash;
	}

	public static String releaseNumber() {
		String releaseNumber = getFirstStringTokenFromResource(CURRENT_RELEASE_NUMBER_RESOURCE);
		
		if (releaseNumber == null) {
			releaseNumber = "warning: no release number";
			logger.warn("No release number found. The project will not contain software release version.");
		}

		return releaseNumber;
	}

	private static String getFirstStringTokenFromResource(String resource) {
		final InputStream in = HashGenerator.class.getResourceAsStream(resource);
		if (in != null) {
			try (final Scanner scanner = new Scanner(in)) {
				if (scanner.hasNext()) {
					return scanner.next();
				}
			}
		}
		return null;
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
