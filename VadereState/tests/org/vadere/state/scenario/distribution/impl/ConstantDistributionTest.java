package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VDistributionTest;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConstantDistribution.class })
public class ConstantDistributionTest extends VDistributionTest {

	private double updateFrequency = 1;

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		AttributesConstantDistribution parameter = new AttributesConstantDistribution();
		parameter.setUpdateFrequency(updateFrequency);

		ConstantDistribution dist = new ConstantDistribution(parameter,null);

		return dist;
	}

	@Override
	public void testGetNextSpawnTime() throws Exception {
		double timeCurrentEvent = 1;
		VDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSpawnTime(timeCurrentEvent);
		assertEquals(updateFrequency + timeCurrentEvent, actual, 0);
	}

}
