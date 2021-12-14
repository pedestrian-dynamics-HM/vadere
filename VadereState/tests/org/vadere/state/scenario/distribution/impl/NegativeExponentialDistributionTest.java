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
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;
import org.vadere.state.scenario.distribution.parameter.NegativeExponentialParameter;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ NegativeExponentialDistribution.class })
public class NegativeExponentialDistributionTest extends VadereDistributionTest {

	@Mock
	ExponentialDistribution distMock;

	@Override
	protected VadereDistribution<?> getDistributionUnderTest() throws Exception {
		NegativeExponentialParameter parameter = new NegativeExponentialParameter();
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

		VadereDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSpawnTime(timeCurrentEvent);

		assertEquals(sample + timeCurrentEvent, actual, 0);
		Mockito.verify(distMock).sample();
	}

}
