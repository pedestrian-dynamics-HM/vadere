package org.vadere.state.scenario.distribution;

import java.lang.reflect.Constructor;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.scenario.distribution.registry.DistributionRegistry;
import org.vadere.state.scenario.distribution.registry.RegisteredDistribution;

/** @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu) */
public class DistributionFactory {

  public static VDistribution<?> create(
      AttributesDistribution parameters, RandomGenerator randomGenerator) throws Exception {
    RegisteredDistribution registeredDistribution = DistributionRegistry.get(parameters);
    Class<?> pClazz = registeredDistribution.getParameter();

    Constructor<? extends VDistribution<?>> distributionConstructor =
        registeredDistribution.getDistribution().getConstructor(pClazz, RandomGenerator.class);

    return distributionConstructor.newInstance(parameters, randomGenerator);
  }
}
