package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.*;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VDistributionTest;
import org.vadere.state.scenario.distribution.parameter.AttributesNegativeExponentialDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ NegativeExponentialDistribution.class })
public class NegativeExponentialDistributionTest extends VDistributionTest {

	@Mock
	ExponentialDistribution distMock;

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		AttributesNegativeExponentialDistribution parameter = new AttributesNegativeExponentialDistribution();
		parameter.setMean(0);

		RandomGenerator randomGenerator = null;

		PowerMockito.whenNew(ExponentialDistribution.class).withAnyArguments().thenReturn(distMock);

		NegativeExponentialDistribution dist = new NegativeExponentialDistribution(parameter, spawnNumber,
		        randomGenerator);

		PowerMockito.verifyNew(ExponentialDistribution.class).withArguments(randomGenerator, parameter.getMean());

		return dist;
	}

	@Override
	public void testGetNextSpawnTime() throws Exception {
		double sample = 5;
		Mockito.when(distMock.sample()).thenReturn(sample);
		double timeCurrentEvent = 1;

		VDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSpawnTime(timeCurrentEvent);

		assertEquals(sample + timeCurrentEvent, actual, 0);
		Mockito.verify(distMock).sample();
	}

}
