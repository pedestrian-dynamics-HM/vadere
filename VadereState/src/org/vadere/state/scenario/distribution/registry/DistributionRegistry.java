package org.vadere.state.scenario.distribution.registry;

import org.reflections.Reflections;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.scenario.distribution.VDistribution;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Set;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public class DistributionRegistry {

	private static final HashMap<String, RegisteredDistribution> REGISTRY = findDistributions();

	public static RegisteredDistribution get(AttributesDistribution parameters) throws Exception {
		String name = parameters.getClass().getName();
		return get(name);
	}

	public static RegisteredDistribution get(String name) throws Exception {
		if (!REGISTRY.containsKey(name)){
			throw new Exception(
					"There is no distribution with name " + name + ". Possible options are " + getRegisteredNames());
		}
		return REGISTRY.get(name);
	}

	public static String getName(Class attributesDistribution) throws Exception{
		for(var val : REGISTRY.values()){
			if(val.getParameter().equals(attributesDistribution)){
				return val.getDistribution().getName().split(".impl.")[1];
			}
		}
		throw  new Exception(
				"There is no distribution with name " + attributesDistribution + ". Possible options are " + getRegisteredNames());
	}

	public static Set<String> getRegisteredNames() {
		return REGISTRY.keySet();
	}

	private static HashMap<String, RegisteredDistribution> findDistributions() {
		Reflections reflections = new Reflections("org.vadere.state.scenario.distribution.impl");
		Set<Class<?>> distributions = reflections.getTypesAnnotatedWith(RegisterDistribution.class);
		HashMap<String, RegisteredDistribution> registry = new HashMap<String, RegisteredDistribution>();

		distributions.forEach(clazz -> {
			RegisterDistribution annotation = clazz.getAnnotation(RegisterDistribution.class);

			if (VDistribution.class.isAssignableFrom(clazz)) {
				@SuppressWarnings("unchecked") // safe to cast because clazz extends VadereDistribution
				Class<? extends VDistribution<?>> vadereDistributionclazz = (Class<? extends VDistribution<?>>) clazz;
				RegisteredDistribution a = new RegisteredDistribution(annotation.parameter(), vadereDistributionclazz);
				String name = ((ParameterizedType)clazz.getGenericSuperclass()).getActualTypeArguments()[0].getTypeName();
				registry.put(name, a);
			}

			else {
				System.out.println(DistributionRegistry.class.getName() + ": " + clazz
				        + "will be skipped because it does not extend VadereDistribution");
			}

		});

		return registry;
	}

}
