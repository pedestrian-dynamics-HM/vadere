package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.DistributionFactory;
import org.vadere.state.scenario.distribution.DistributionStub;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.VDistributionTest;
import org.vadere.state.scenario.distribution.parameter.AttributesMixedDistribution;
import org.vadere.state.scenario.distribution.parameter.MixedParameterDistribution;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ MixedDistribution.class, DistributionFactory.class })
public class MixedDistributionTest extends VDistributionTest {

	RandomGenerator randomGenerator = null;
	VDistribution<?> distStub;

	public MixedDistributionTest() throws Exception {
		distStub = new DistributionStub(null, 0, randomGenerator);
	}

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		MixedParameterDistribution parameterDist = new MixedParameterDistribution();
		parameterDist.setInterSpawnTimeDistribution("");
		parameterDist.setDistributionParameters(Mockito.mock(JsonNode.class));

		ArrayList<MixedParameterDistribution> dists = new ArrayList<>();
		dists.add(parameterDist);

		AttributesMixedDistribution parameter = new AttributesMixedDistribution();
		parameter.setDistributions(dists);

		double[] switchpoints = {};
		parameter.setSwitchpoints(switchpoints);

		PowerMockito.mockStatic(DistributionFactory.class);
		PowerMockito.doReturn(distStub).when(DistributionFactory.class);
		DistributionFactory.create(parameterDist.getInterSpawnTimeDistribution(),
		        parameterDist.getDistributionParameters(), spawnNumber, randomGenerator);

		MixedDistribution dist = new MixedDistribution(parameter, spawnNumber, null);

		return dist;
	}

	@Override
	public void testGetSpawnNumber() throws Exception {
		VDistribution<?> dist = getDistributionUnderTest();
		int actual = dist.getSpawnNumber(1);
		assertEquals(distStub.getSpawnNumber(1), actual);
	}

	@Override
	public void testRemainingSpawnAgents() throws Exception {
		VDistribution<?> dist = getDistributionUnderTest();
		dist.setRemainingSpawnAgents(1);
		int actual = dist.getRemainingSpawnAgents();
		assertEquals(distStub.getRemainingSpawnAgents(), actual);
	}

	@Override
	public void testGetNextSpawnTime() throws Exception {
		VDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSpawnTime(12);
		assertEquals(distStub.getNextSpawnTime(12), actual, 0);
	}

}
