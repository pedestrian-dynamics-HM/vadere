package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.util.Attributes;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.attributes.distributions.AttributesMixedDistribution;
import org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution;
import org.vadere.state.attributes.distributions.AttributesTimeSeriesDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.parameter.*;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "timeSeries", parameter = AttributesTimeSeriesDistribution.class)
public class TimeSeriesDistribution extends VDistribution<AttributesTimeSeriesDistribution> {

	private Attributes timeSeriesAttributes;
	private MixedDistribution distribution;

	public TimeSeriesDistribution(){
		// Do not remove this contructor. It is us used through reflection.
		super();
		this.timeSeriesAttributes = new AttributesTimeSeriesDistribution();
	}
	public TimeSeriesDistribution(AttributesTimeSeriesDistribution parameter, int spawnNumber, RandomGenerator unused)
	        throws Exception {
		super(parameter,unused);
	}
/*
	@Override
	public int getSpawnNumber(double timeCurrentEvent) {
		return distribution.getSpawnNumber(timeCurrentEvent);
	}
*/
	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return distribution.getNextSpawnTime(timeCurrentEvent);
	}
/*
	@Override
	public int getRemainingSpawnAgents() {
		return distribution.getRemainingSpawnAgents();
	}

	@Override
	public void setRemainingSpawnAgents(int remainingAgents) {
		distribution.setRemainingSpawnAgents(remainingAgents);
	}
*/
	@Override
	protected void setValues(AttributesTimeSeriesDistribution parameter,RandomGenerator unused2) throws Exception {
		ArrayList<Integer> spawnsPerInterval = parameter.getSpawnsPerInterval();
		ArrayList<Double> switchpoints = new ArrayList<>();
		ArrayList<MixedParameterDistribution> distributions = new ArrayList<>();

		ObjectMapper mapper = new ObjectMapper();

		double intervalLength = parameter.getIntervalLength();
		double currentTime = intervalLength;

		for (int i = 0; i < spawnsPerInterval.size(); i++) {
			currentTime = addNewDistribution(spawnsPerInterval, intervalLength, mapper, switchpoints, distributions, currentTime, i);
		}

		AttributesMixedDistribution mixedP = new AttributesMixedDistribution();
		mixedP.setSwitchpoints(switchpoints);
		mixedP.setDistributions(distributions);
		distribution = new MixedDistribution(mixedP, 1, unused2);

	}

	private static double addNewDistribution(ArrayList<Integer> spawnsPerInterval, double intervalLength, ObjectMapper mapper, ArrayList<Double> switchpoints, ArrayList<MixedParameterDistribution> distributions, double currentTime, int i) {
		MixedParameterDistribution dist = new MixedParameterDistribution();

		currentTime = addSwitchpointIfNotLastInterval(spawnsPerInterval, intervalLength, switchpoints, currentTime, i);

		final int spawns = spawnsPerInterval.get(i);
		final boolean spawnsSetToRepeat = spawns > 0;

		if (spawnsSetToRepeat) {
			initAsConstantDistribution(intervalLength, mapper, spawns, dist);
		} else {
			initAsSingleEventDistribution(spawnsPerInterval, intervalLength, mapper, currentTime, i, dist);
		}
		distributions.add(dist);
		return currentTime;
	}

	private static void initAsSingleEventDistribution(ArrayList<Integer> spawnsPerInterval, double intervalLength, ObjectMapper mapper, double currentTime, int i, MixedParameterDistribution dist) {
		AttributesSingleSpawnDistribution singleP = new AttributesSingleSpawnDistribution();
		final boolean iterIsAtLastPos = i == spawnsPerInterval.size() - 1;
		if (iterIsAtLastPos) {
			singleP.setSpawnTime(Double.MAX_VALUE);
		} else {
			singleP.setSpawnTime(currentTime - intervalLength);
		}
		JsonNode node = mapper.convertValue(singleP, JsonNode.class);
		dist.setInterSpawnTimeDistribution("singleSpawn");
		dist.setDistributionParameters(node);
	}

	private static void initAsConstantDistribution(double intervalLength, ObjectMapper mapper, int spawns, MixedParameterDistribution dist) {
		AttributesConstantDistribution constantP = new AttributesConstantDistribution();
		constantP.setUpdateFrequency(intervalLength / spawns);
		JsonNode node = mapper.convertValue(constantP, JsonNode.class);
		dist.setInterSpawnTimeDistribution("constant");
		dist.setDistributionParameters(node);
	}

	private static double addSwitchpointIfNotLastInterval(ArrayList<Integer> spawnsPerIntveral, double intervalLength, ArrayList<Double> switchpoints, double currentTime, int i) {
		final boolean iterIsNotLast = i < spawnsPerIntveral.size() - 1;
		if (iterIsNotLast) {
			switchpoints.add(currentTime);
			currentTime += intervalLength;
		}
		return currentTime;
	}

	@Override
	public Attributes getAttributes() {
		return this.timeSeriesAttributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		if(attributes instanceof AttributesTimeSeriesDistribution)
			this.timeSeriesAttributes = attributes;
		else
			throw new IllegalArgumentException();
	}
}
