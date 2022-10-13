package org.vadere.state.scenario.distribution.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.distributions.*;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "timeSeries", parameter = AttributesTimeSeriesDistribution.class)
public class TimeSeriesDistribution extends VDistribution<AttributesTimeSeriesDistribution> {

	private Attributes timeSeriesAttributes;
	private MixedDistribution distribution;

	public TimeSeriesDistribution(AttributesTimeSeriesDistribution parameter, RandomGenerator unused)
	        throws Exception {
		super(parameter,unused);
	}

	@Override
	public double getNextSample(double timeCurrentEvent) {
		return distribution.getNextSample(timeCurrentEvent);
	}

	@Override
	protected void setValues(AttributesTimeSeriesDistribution parameter,RandomGenerator unused2) throws Exception {
		ArrayList<Integer> spawnsPerInterval =(ArrayList<Integer>) parameter.getSpawnsPerInterval();
		ArrayList<Double> switchpoints = new ArrayList<>();
		List<AttributesDistribution> distributions = new ArrayList<>();

		ObjectMapper mapper = new ObjectMapper();

		double intervalLength = parameter.getIntervalLength();
		double currentTime = intervalLength;

		for (int i = 0; i < spawnsPerInterval.size(); i++) {
			currentTime = addNewDistribution(spawnsPerInterval, intervalLength, mapper, switchpoints, distributions, currentTime, i);
		}

		AttributesMixedDistribution mixedP = new AttributesMixedDistribution();
		mixedP.setSwitchpoints(switchpoints);
		mixedP.setDistributions(distributions);
		distribution = new MixedDistribution(mixedP,unused2);

	}

	private static double addNewDistribution(ArrayList<Integer> spawnsPerInterval, double intervalLength, ObjectMapper mapper, ArrayList<Double> switchpoints, List<AttributesDistribution> distributions, double currentTime, int i) {
		AttributesDistribution dist;

		currentTime = addSwitchpointIfNotLastInterval(spawnsPerInterval, intervalLength, switchpoints, currentTime, i);

		final int spawns = spawnsPerInterval.get(i);
		final boolean spawnsSetToRepeat = spawns > 0;

		if (spawnsSetToRepeat) {
			dist = initAsConstantDistribution(intervalLength,spawns);
		} else {
			dist = initAsSingleEventDistribution(spawnsPerInterval, intervalLength, currentTime, i);
		}
		distributions.add(dist);
		return currentTime;
	}

	private static AttributesSingleSpawnDistribution initAsSingleEventDistribution(ArrayList<Integer> spawnsPerInterval, double intervalLength,double currentTime, int i) {
		AttributesSingleSpawnDistribution singleP = new AttributesSingleSpawnDistribution();
		final boolean iterIsAtLastPos = i == spawnsPerInterval.size() - 1;
		if (iterIsAtLastPos) {
			singleP.setSpawnTime(Double.MAX_VALUE);
		} else {
			singleP.setSpawnTime(currentTime - intervalLength);
		}
		return singleP;
	}

	private static AttributesConstantDistribution initAsConstantDistribution(double intervalLength,int spawns) {
		AttributesConstantDistribution constantP = new AttributesConstantDistribution();
		constantP.setUpdateFrequency(intervalLength / spawns);
		return constantP;
	}

	private static double addSwitchpointIfNotLastInterval(ArrayList<Integer> spawnsPerIntveral, double intervalLength, ArrayList<Double> switchpoints, double currentTime, int i) {
		final boolean iterIsNotLast = i < spawnsPerIntveral.size() - 1;
		if (iterIsNotLast) {
			switchpoints.add(currentTime);
			currentTime += intervalLength;
		}
		return currentTime;
	}
}
