package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;
import org.vadere.state.scenario.distribution.parameter.BinomialParameter;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BinomialDistribution.class })
public class BinomialDistributionTest extends VadereDistributionTest {

	@Mock
	org.apache.commons.math3.distribution.BinomialDistribution distMock;

	@Override
	protected VadereDistribution<?> getDistributionUnderTest() throws Exception {
		BinomialParameter parameter = new BinomialParameter();
		parameter.setP(1);
		parameter.setTrials(2);

		RandomGenerator randomGenerator = null;

		PowerMockito.whenNew(org.apache.commons.math3.distribution.BinomialDistribution.class).withAnyArguments()
		        .thenReturn(distMock);

		BinomialDistribution dist = new BinomialDistribution(parameter, spawnNumber, randomGenerator);

		PowerMockito.verifyNew(org.apache.commons.math3.distribution.BinomialDistribution.class)
		        .withArguments(randomGenerator, parameter.getTrials(), parameter.getP());

		return dist;
	}

	@Override
	public void testGetNextSpawnTime() throws Exception {
		int sample = 5;
		Mockito.when(distMock.sample()).thenReturn(sample);
		double timeCurrentEvent = 1;

		VadereDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSpawnTime(timeCurrentEvent);

		assertEquals(sample + timeCurrentEvent, actual, 0);
		Mockito.verify(distMock).sample();

	}

}
