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
import org.vadere.state.scenario.distribution.parameter.PoissonParameter;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PoissonDistribution.class })
public class PoissonDistributionTest extends VadereDistributionTest {

	@Mock
	ExponentialDistribution distMock;

	@Override
	protected VadereDistribution<?> getDistributionUnderTest() throws Exception {
		PoissonParameter parameter = new PoissonParameter();
		parameter.setOccurrencesPerSecond(0);

		RandomGenerator randomGenerator = null;

		PowerMockito.whenNew(ExponentialDistribution.class).withAnyArguments().thenReturn(distMock);

		PoissonDistribution dist = new PoissonDistribution(parameter, spawnNumber, randomGenerator);

		PowerMockito.verifyNew(ExponentialDistribution.class).withArguments(randomGenerator,
		        1 / parameter.getOccurrencesPerSecond());

		return dist;
	}

	@Override
	public void testGetNextSpawnTime() throws Exception {
		double sample = 5;
		Mockito.when(distMock.sample()).thenReturn(sample);
		double timeCurrentEvent = 1;

		VadereDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSample(timeCurrentEvent);

		assertEquals(sample + timeCurrentEvent, actual, 0);
		Mockito.verify(distMock).sample();
	}

}
