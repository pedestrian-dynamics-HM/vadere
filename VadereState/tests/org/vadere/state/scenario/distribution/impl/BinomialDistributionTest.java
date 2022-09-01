package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VDistributionTest;
import org.vadere.state.attributes.distributions.AttributesBinomialDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BinomialDistribution.class })
public class BinomialDistributionTest extends VDistributionTest {

	@Mock
	org.apache.commons.math3.distribution.BinomialDistribution distMock;

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		AttributesBinomialDistribution parameter = new AttributesBinomialDistribution();
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

		VDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSpawnTime(timeCurrentEvent);

		assertEquals(sample + timeCurrentEvent, actual, 0);
		Mockito.verify(distMock).sample();

	}

}
