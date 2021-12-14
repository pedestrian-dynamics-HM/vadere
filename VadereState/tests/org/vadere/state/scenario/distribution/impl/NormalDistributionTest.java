package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.*;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;
import org.vadere.state.scenario.distribution.parameter.NormalParameter;
import org.vadere.util.math.TruncatedNormalDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ NormalDistribution.class })
public class NormalDistributionTest extends VadereDistributionTest {

	@Mock
	TruncatedNormalDistribution distMock;

	@Override
	protected VadereDistribution<?> getDistributionUnderTest() throws Exception {
		NormalParameter parameter = new NormalParameter();
		parameter.setMean(1);
		parameter.setSd(2);

		PowerMockito.whenNew(TruncatedNormalDistribution.class).withAnyArguments().thenReturn(distMock);

		RandomGenerator randomGenerator = null;

		NormalDistribution dist = new NormalDistribution(parameter, spawnNumber, randomGenerator);

		PowerMockito.verifyNew(TruncatedNormalDistribution.class).withArguments(randomGenerator, parameter.getMean(),
		        parameter.getSd(), 0d, Double.MAX_VALUE, 1000);

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
