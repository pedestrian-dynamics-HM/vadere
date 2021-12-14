package org.vadere.state.scenario.distribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.scenario.distribution.registry.DistributionRegistry;
import org.vadere.state.scenario.distribution.registry.RegisteredDistribution;

import java.lang.reflect.Constructor;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
public class DistributionFactory {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static VadereDistribution<?> create(String name, JsonNode parameters, int spawnNumber,
	        RandomGenerator randomGenerator) throws Exception {
		RegisteredDistribution registeredDistribution = DistributionRegistry.get(name);
		Class<?> pClazz = registeredDistribution.getParameter();

		Constructor<? extends VadereDistribution<?>> distributionConstructor = registeredDistribution.getDistribution()
		        .getConstructor(pClazz, int.class, RandomGenerator.class);

		Object p = map(parameters, pClazz);
		return distributionConstructor.newInstance(p, spawnNumber, randomGenerator);
	}

	private static <T> T map(JsonNode source, Class<T> target) throws Exception {
		try {
			return OBJECT_MAPPER.readValue(source.toString(), target);
		} catch (Exception e) {
			String name = target.getName();
			throw new Exception(
			        "An Error occured while parsing" + name + ". Scenario file is misconfigured. Error: " + e);
		}
	}

}
