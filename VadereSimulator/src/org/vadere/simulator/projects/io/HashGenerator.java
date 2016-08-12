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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HashGenerator {

	private static Logger logger = LogManager.getLogger(HashGenerator.class);
	
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
		InputStream in = HashGenerator.class.getResourceAsStream("/current_commit_hash.txt");
		boolean result = in != null;
		if(result) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public static String commitHash() {
		InputStream in = HashGenerator.class.getResourceAsStream("/current_commit_hash.txt");
		String commithash = "";

		if(in != null) {
			Scanner scanner = new Scanner(HashGenerator.class.getResourceAsStream("/current_commit_hash.txt"));
			if(scanner.hasNext()) {
				commithash = scanner.next();
			}
			else {
				logger.warn("no commit hash in resource.");
			}
			scanner.close();
		}
		else {
			commithash = "warning: no commit hash";
			logger.warn("no commit hash. This will cause the scenario output file to be not uniquely assignable to a software version.");
		}

		return commithash;
	}

	public static String releaseNumber() {
		InputStream in = HashGenerator.class.getResourceAsStream("/current_release_number.txt");
		String releaseNumber = "";
		if(in != null) {
			Scanner scanner = new Scanner(HashGenerator.class.getResourceAsStream("/current_release_number.txt"));
			releaseNumber = scanner.next();
			if(scanner.hasNext()) {
				releaseNumber = scanner.next();
			}
			else {
				logger.warn("no release number in resource.");
			}
			scanner.close();
		}
		else {
			releaseNumber = "warning: no release number";
			logger.warn("no release number. This will cause the project files to be not uniquely assignable to a software release version.");
		}


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
