package org.vadere.state.scenario.distribution.impl;

import org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VDistributionTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public class SingleSpawnDistributionTest extends VDistributionTest {

	private final double spawnTime = 2;

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
		double actual = dist.getNextSample(12);
		assertEquals(spawnTime, actual, 0);
	}

}
