package org.vadere.state.scenario;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class DistributionFactory {

	private String distributionCanonicalPath;

	public DistributionFactory(String distributionCanonicalPath) {
		this.distributionCanonicalPath = distributionCanonicalPath;

	}

	public SpawnDistribution createDistribution(Random random, int spawnNumber, List<Double> parameters)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			SecurityException {

		RandomGenerator randomGenerator = new JDKRandomGenerator(random.nextInt());
		SpawnDistribution returnDistribution;

		if (distributionCanonicalPath .equals("org.vadere.state.scenario.ConstantDistributionReplace")){
			returnDistribution = new ConstantDistributionReplace(randomGenerator, spawnNumber, parameters);
		}else{
			throw new IllegalArgumentException(distributionCanonicalPath  + " not known in DistributionFactory");
		}

		return returnDistribution;

	}

}
