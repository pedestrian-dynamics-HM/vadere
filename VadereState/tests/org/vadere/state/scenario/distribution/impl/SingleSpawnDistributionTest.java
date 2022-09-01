package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.*;

import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VDistributionTest;
import org.vadere.state.scenario.distribution.parameter.AttributesSingleSpawnDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public class SingleSpawnDistributionTest extends VDistributionTest {

	private double spawnTime = 2;

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		AttributesSingleSpawnDistribution parameter = new AttributesSingleSpawnDistribution();
		parameter.setSpawnNumber(1);
		parameter.setSpawnTime(spawnTime);

		SingleSpawnDistribution dist = new SingleSpawnDistribution(parameter, spawnNumber, null);

		return dist;
	}

	@Override
	public void testGetNextSpawnTime() throws Exception {
		VDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSpawnTime(12);
		assertEquals(spawnTime, actual, 0);
	}

}
