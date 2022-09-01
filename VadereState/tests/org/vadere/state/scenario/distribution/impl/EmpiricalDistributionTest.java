package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.*;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VDistributionTest;
import org.vadere.state.scenario.distribution.parameter.AttributesEmpiricalDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ EmpiricalDistribution.class })
public class EmpiricalDistributionTest extends VDistributionTest {

	@Mock
	org.apache.commons.math3.random.EmpiricalDistribution distMock;

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		AttributesEmpiricalDistribution parameter = new AttributesEmpiricalDistribution();
		parameter.setValues(null);

		RandomGenerator randomGenerator = null;

		PowerMockito.whenNew(org.apache.commons.math3.random.EmpiricalDistribution.class).withAnyArguments()
		        .thenReturn(distMock);

		EmpiricalDistribution dist = new EmpiricalDistribution(parameter, spawnNumber, randomGenerator);

		PowerMockito.verifyNew(org.apache.commons.math3.random.EmpiricalDistribution.class)
		        .withArguments(randomGenerator);
		//Mockito.verify(distMock).load(parameter.getValues());

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
