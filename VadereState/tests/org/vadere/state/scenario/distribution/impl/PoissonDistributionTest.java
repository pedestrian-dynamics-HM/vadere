package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.attributes.distributions.AttributesPoissonDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;

/** @author Aleksandar Ivanov(ivanov0@hm.edu) */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PoissonDistribution.class})
public class PoissonDistributionTest extends VadereDistributionTest {

  @Mock ExponentialDistribution distMock;

  @Override
  protected VDistribution<?> getDistributionUnderTest() throws Exception {
    AttributesPoissonDistribution parameter = new AttributesPoissonDistribution();
    parameter.setNumberPedsPerSecond(0);

    RandomGenerator randomGenerator = null;

    PowerMockito.whenNew(ExponentialDistribution.class).withAnyArguments().thenReturn(distMock);

    PoissonDistribution dist = new PoissonDistribution(parameter, randomGenerator);

    PowerMockito.verifyNew(ExponentialDistribution.class)
        .withArguments(randomGenerator, 1 / parameter.getNumberPedsPerSecond());

    return dist;
  }

  @Override
  public void testGetNextSample() throws Exception {
    double sample = 5;
    Mockito.when(distMock.sample()).thenReturn(sample);
    double timeCurrentEvent = 1;

    VDistribution<?> dist = getDistributionUnderTest();
    double actual = dist.getNextSample(timeCurrentEvent);

    assertEquals(sample + timeCurrentEvent, actual, 0);
    Mockito.verify(distMock).sample();
  }
}
