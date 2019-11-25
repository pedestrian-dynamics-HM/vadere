package org.vadere.state.scenario;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class DistributionFactory {

	private String distributionCanonicalPath;

	public DistributionFactory(String distributionCanonicalPath) throws ClassNotFoundException {
		Class<?> clazz = Class.forName(distributionCanonicalPath);
		this.distributionCanonicalPath = distributionCanonicalPath;

	}

	public SpawnDistribution createDistribution(Random random, int spawnNumber, List<Double> parameters)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			SecurityException {

		RandomGenerator randomGenerator = new JDKRandomGenerator(random.nextInt());
		SpawnDistribution returnDistribution;

		if (distributionCanonicalPath.equals("org.vadere.state.scenario.ConstantDistribution")){
			returnDistribution = new ConstantDistribution(randomGenerator, spawnNumber, parameters);
		}else if(distributionCanonicalPath.equals("org.vadere.state.scenario.LinearInterpolationSpawnDistribution")){
			returnDistribution = new LinearInterpolationSpawnDistribution(randomGenerator, parameters);
		}else{
			throw new IllegalArgumentException(distributionCanonicalPath  + " not known in DistributionFactory");
		}

		return returnDistribution;

	}

}
