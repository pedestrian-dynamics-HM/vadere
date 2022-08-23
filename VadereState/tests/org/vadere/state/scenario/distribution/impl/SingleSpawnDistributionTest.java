package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.*;

import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;
import org.vadere.state.scenario.distribution.parameter.SingleEventParameter;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public class SingleSpawnDistributionTest extends VadereDistributionTest {

	private double spawnTime = 2;

	@Override
	protected VadereDistribution<?> getDistributionUnderTest() throws Exception {
		SingleEventParameter parameter = new SingleEventParameter();
		parameter.setSpawnNumber(1);
		parameter.setEventTime(spawnTime);

		SingleEventDistribution dist = new SingleEventDistribution(parameter, spawnNumber, null);

		return dist;
	}

	@Override
	public void testGetNextSpawnTime() throws Exception {
		VadereDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSample(12);
		assertEquals(spawnTime, actual, 0);
	}

}
