package org.vadere.state.scenario.distribution.impl;

import org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;

import static org.junit.Assert.assertEquals;


/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public class SingleSpawnDistributionTest extends VadereDistributionTest {

	private final double spawnTime = 2;

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		AttributesSingleSpawnDistribution parameter = new AttributesSingleSpawnDistribution();
		parameter.setSpawnTime(spawnTime);

		SingleSpawnDistribution dist = new SingleSpawnDistribution(parameter,null);

		return dist;
	}

	@Override
	public void testGetNextSample() throws Exception {
		VDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSample(12);
		assertEquals(spawnTime, actual, 0);
	}

}
