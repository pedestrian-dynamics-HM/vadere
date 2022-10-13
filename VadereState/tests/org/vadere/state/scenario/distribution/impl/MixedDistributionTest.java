package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.distributions.AttributesMixedDistribution;
import org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution;
import org.vadere.state.scenario.distribution.DistributionFactory;
import org.vadere.state.scenario.distribution.DistributionStub;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ MixedDistribution.class, DistributionFactory.class })
public class MixedDistributionTest extends VadereDistributionTest {

	RandomGenerator randomGenerator = null;
	VDistribution<?> distStub;

	public MixedDistributionTest() throws Exception {
		distStub = new DistributionStub(null,  randomGenerator);
	}

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		SingleSpawnDistribution subDistr = new SingleSpawnDistribution(Mockito.mock(AttributesSingleSpawnDistribution.class),null);

		ArrayList<AttributesDistribution> dists = new ArrayList<>();
		dists.add(subDistr.getAttributes());

		AttributesMixedDistribution parameter = new AttributesMixedDistribution();
		parameter.setDistributions(dists);

		ArrayList<Double> switchpoints = new ArrayList<>();
		parameter.setSwitchpoints(switchpoints);

		PowerMockito.mockStatic(DistributionFactory.class);
		PowerMockito.doReturn(distStub).when(DistributionFactory.class);
		DistributionFactory.create(subDistr.getAttributes(), randomGenerator);

		MixedDistribution dist = new MixedDistribution(parameter,  null);

		return dist;
	}
/*
	@Override
	public void testGetSpawnNumber() throws Exception {
		VadereDistribution<?> dist = getDistributionUnderTest();
		int actual = dist.getSpawnNumber(1);
		assertEquals(distStub.getSpawnNumber(1), actual);
	}
*/
	/*
	@Override
	public void testRemainingSpawnAgents() throws Exception {
		VadereDistribution<?> dist = getDistributionUnderTest();
		dist.setRemainingSpawnAgents(1);
		int actual = dist.getRemainingSpawnAgents();
		assertEquals(distStub.getRemainingSpawnAgents(), actual);
	}
*/
	@Override
	public void testGetNextSample() throws Exception {
		VDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSample(12);
		assertEquals(distStub.getNextSample(12), actual, 0);
	}

}
