package org.vadere.state.scenario.distribution.impl;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;

import static org.junit.Assert.assertEquals;


/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConstantDistribution.class })
public class ConstantDistributionTest extends VadereDistributionTest {

	private final double updateFrequency = 1;

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		AttributesConstantDistribution parameter = new AttributesConstantDistribution();
		parameter.setUpdateFrequency(updateFrequency);

		ConstantDistribution dist = new ConstantDistribution(parameter, null);

		return dist;
	}

	@Override
	public void testGetNextSample() throws Exception {
		double timeCurrentEvent = 1;
		VDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSample(timeCurrentEvent);
		assertEquals(updateFrequency + timeCurrentEvent, actual, 0);
	}

}
