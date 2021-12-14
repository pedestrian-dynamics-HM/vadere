package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.parameter.*;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "timeSeries", parameter = TimeSeriesParameter.class)
public class TimeSeriesDistribution extends VadereDistribution<TimeSeriesParameter> {

	private MixedDistribution distribution;

	public TimeSeriesDistribution(TimeSeriesParameter parameter, int spawnNumber, RandomGenerator unused)
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
	protected void setValues(TimeSeriesParameter parameter, int unused1, RandomGenerator unused2) throws Exception {
		int[] spawnsPerIntveral = parameter.getSpawnsPerInterval();
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
				ConstantParameter constantP = new ConstantParameter();
				constantP.setUpdateFrequency(intervalLength / spawns);
				JsonNode node = mapper.convertValue(constantP, JsonNode.class);
				dist.setInterSpawnTimeDistribution("constant");
				dist.setDistributionParameters(node);
			} else {
				SingleSpawnParameter singleP = new SingleSpawnParameter();
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

		MixedParameter mixedP = new MixedParameter();
		mixedP.setSwitchpoints(switchpoints);
		mixedP.setDistributions(distributions);
		distribution = new MixedDistribution(mixedP, 1, unused2);

	}
}
