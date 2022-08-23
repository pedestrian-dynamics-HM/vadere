package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;
import org.vadere.state.scenario.distribution.parameter.ConstantParameter;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConstantDistribution.class })
public class ConstantDistributionTest extends VadereDistributionTest {

	private double updateFrequency = 1;

	@Override
	protected VadereDistribution<?> getDistributionUnderTest() throws Exception {
		ConstantParameter parameter = new ConstantParameter();
		parameter.setTimeInterval(updateFrequency);

		ConstantDistribution dist = new ConstantDistribution(parameter, spawnNumber, null);

		return dist;
	}

	@Override
	public void testGetNextSpawnTime() throws Exception {
		double timeCurrentEvent = 1;
		VadereDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSample(timeCurrentEvent);
		assertEquals(updateFrequency + timeCurrentEvent, actual, 0);
	}

}
