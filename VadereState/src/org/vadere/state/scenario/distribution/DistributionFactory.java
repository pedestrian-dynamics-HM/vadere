package org.vadere.state.scenario.distribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.scenario.distribution.registry.DistributionRegistry;
import org.vadere.state.scenario.distribution.registry.RegisteredDistribution;

import java.lang.reflect.Constructor;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
public class DistributionFactory {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static VDistribution<?> create(AttributesDistribution parameters,
                                          RandomGenerator randomGenerator) throws Exception {
		RegisteredDistribution registeredDistribution = DistributionRegistry.get(parameters);
		Class<?> pClazz = registeredDistribution.getParameter();

		Constructor<? extends VDistribution<?>> distributionConstructor = registeredDistribution.getDistribution()
		        .getConstructor(pClazz,RandomGenerator.class);

		//Object p = map(parameters, pClazz);
		return distributionConstructor.newInstance(parameters,randomGenerator);
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
