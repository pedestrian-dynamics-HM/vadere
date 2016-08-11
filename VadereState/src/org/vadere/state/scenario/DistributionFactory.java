package org.vadere.state.scenario;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class DistributionFactory {

	private Class<? extends RealDistribution> distributionClass;

	public DistributionFactory(Class<? extends RealDistribution> distributionClass) {
		this.distributionClass = distributionClass;
	}

	public static DistributionFactory fromDistributionClassName(String className) throws ClassNotFoundException {
		@SuppressWarnings("unchecked")
		Class<? extends RealDistribution> distributionClass =
				(Class<? extends RealDistribution>) Class.forName(className);
		// This cast does not throw a ClassCastException :( I don't know why
		// A wrong class here comes back later in createDistribution
		return new DistributionFactory(distributionClass);
	}

	public RealDistribution createDistribution(Random random, List<Double> parameters)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			SecurityException {

		final List<Object> argList = new LinkedList<>(parameters);
		argList.add(0, new JDKRandomGenerator(random.nextInt()));

		final int argCount = argList.size();
		final Object[] args = argList.toArray(new Object[argCount]);

		final Class<?>[] argTypes = new Class<?>[argCount];
		argTypes[0] = RandomGenerator.class;
		Arrays.fill(argTypes, 1, argTypes.length, double.class);

		Constructor<?> constructor = distributionClass.getConstructor(argTypes);
		return (RealDistribution) constructor.newInstance(args);
	}

	public RealDistribution createDistribution(Random random, Double... parameters)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			SecurityException {

		List<Double> list = Arrays.asList(parameters);
		return createDistribution(random, list);
	}

}
