package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.distribution.DistributionFactory;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.parameter.AttributesMixedDistribution;
import org.vadere.state.scenario.distribution.parameter.MixedParameterDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

import java.util.ArrayList;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "mixed", parameter = AttributesMixedDistribution.class)
public class MixedDistribution extends VDistribution<AttributesMixedDistribution> {
	private Attributes mixedAttributes;
	Double[] switchpoints;
	ArrayList<VDistribution<?>> distributions;
	private int currentInterval = 0;

	public MixedDistribution(AttributesMixedDistribution parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		super(parameter, spawnNumber, randomGenerator);
	}

	@Override
	protected void setValues(AttributesMixedDistribution parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		if (parameter.getSwitchpoints().length != parameter.getDistributions().size() - 1) {
			throw new Exception("There should be exactly one switchpoint for"
			        + "every given distribution minus 1. However there are not.");
		}

		setDistributions(parameter.getDistributions(), spawnNumber, randomGenerator);
		this.switchpoints = parameter.getSwitchpoints();

	}

	private void setDistributions(ArrayList<MixedParameterDistribution> distributions, int spawnNumber,
	        RandomGenerator randomGenerator) throws Exception {
		this.distributions = new ArrayList<>();

		for (MixedParameterDistribution distribution : distributions) {
			VDistribution<?> dist = DistributionFactory.create(distribution.getInterSpawnTimeDistribution(),
			        distribution.getDistributionParameters(), spawnNumber, randomGenerator);
			this.distributions.add(dist);
		}
	}

	@Override
	public int getSpawnNumber(double timeCurrentEvent) {
		return getDistributionByTime(timeCurrentEvent).getSpawnNumber(timeCurrentEvent);
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return getDistributionByTime(timeCurrentEvent).getNextSpawnTime(timeCurrentEvent);
	}

	@Override
	public int getRemainingSpawnAgents() {
		return distributions.get(currentInterval).getRemainingSpawnAgents();
	}

	@Override
	public void setRemainingSpawnAgents(int remainingAgents) {
		distributions.get(currentInterval).setRemainingSpawnAgents(remainingAgents);
	}

	private VDistribution<?> getDistributionByTime(double timeCurrentEvent) {
		while (!(currentInterval > switchpoints.length - 1) && timeCurrentEvent >= switchpoints[currentInterval]
		        && !(timeCurrentEvent > switchpoints[switchpoints.length - 1])) {
			currentInterval++;
		}

		return distributions.get(currentInterval);
	}

	public VDistribution<?> getCurrentDistribution() {
		return distributions.get(currentInterval);
	}

	@Override
	public Attributes getAttributes() {
		return this.mixedAttributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		if(attributes instanceof AttributesMixedDistribution)
			this.mixedAttributes = attributes;
		else
			throw new IllegalArgumentException();
	}
}
