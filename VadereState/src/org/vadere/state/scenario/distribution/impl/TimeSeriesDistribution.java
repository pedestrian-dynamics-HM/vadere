package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.Attributes;
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

	public TimeSeriesDistribution(AttributesTimeSeriesDistribution parameter, int spawnNumber, RandomGenerator unused)
	        throws Exception {
		super(parameter, spawnNumber, unused);
	}

	@Override
	public int getSpawnNumber(double timeCurrentEvent) {
		return distribution.getSpawnNumber(timeCurrentEvent);
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return distribution.getNextSpawnTime(timeCurrentEvent);
	}

	@Override
	public int getRemainingSpawnAgents() {
		return distribution.getRemainingSpawnAgents();
	}

	@Override
	public void setRemainingSpawnAgents(int remainingAgents) {
		distribution.setRemainingSpawnAgents(remainingAgents);
	}

	@Override
	protected void setValues(AttributesTimeSeriesDistribution parameter, int unused1, RandomGenerator unused2) throws Exception {
		Integer[] spawnsPerIntveral = parameter.getSpawnsPerInterval();
		double intervalLength = parameter.getIntervalLength();

		ObjectMapper mapper = new ObjectMapper();

		double[] switchpoints = new double[spawnsPerIntveral.length - 1];
		ArrayList<MixedParameterDistribution> distributions = new ArrayList<>();
		double currentTime = intervalLength;
		for (int i = 0; i < spawnsPerIntveral.length; i++) {
			if (i < spawnsPerIntveral.length - 1) {
				switchpoints[i] = currentTime;
				currentTime += intervalLength;
			}
			int spawns = spawnsPerIntveral[i];
			MixedParameterDistribution dist = new MixedParameterDistribution();
			if (spawns > 0) {
				AttributesConstantDistribution constantP = new AttributesConstantDistribution();
				constantP.setUpdateFrequency(intervalLength / spawns);
				JsonNode node = mapper.convertValue(constantP, JsonNode.class);
				dist.setInterSpawnTimeDistribution("constant");
				dist.setDistributionParameters(node);
			} else {
				AttributesSingleSpawnDistribution singleP = new AttributesSingleSpawnDistribution();
				if (i == spawnsPerIntveral.length - 1) {
					singleP.setSpawnTime(Double.MAX_VALUE);
				} else {
					singleP.setSpawnTime(currentTime - intervalLength);
				}
				singleP.setSpawnNumber(0);
				JsonNode node = mapper.convertValue(singleP, JsonNode.class);
				dist.setInterSpawnTimeDistribution("singleSpawn");
				dist.setDistributionParameters(node);
			}

			distributions.add(dist);
		}

		AttributesMixedDistribution mixedP = new AttributesMixedDistribution();
		//mixedP.setSwitchpoints(switchpoints);
		mixedP.setDistributions(distributions);
		distribution = new MixedDistribution(mixedP, 1, unused2);

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
