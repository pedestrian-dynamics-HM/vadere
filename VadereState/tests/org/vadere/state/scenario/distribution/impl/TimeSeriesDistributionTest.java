package org.vadere.state.scenario.distribution.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.VadereDistributionTest;
import org.vadere.state.scenario.distribution.parameter.ConstantParameter;
import org.vadere.state.scenario.distribution.parameter.MixedParameter;
import org.vadere.state.scenario.distribution.parameter.MixedParameterDistribution;
import org.vadere.state.scenario.distribution.parameter.SingleSpawnParameter;
import org.vadere.state.scenario.distribution.parameter.TimeSeriesParameter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TimeSeriesDistribution.class })
public class TimeSeriesDistributionTest extends VadereDistributionTest {

	@Mock
	MixedDistribution distMock;

	@Override
	protected VadereDistribution<?> getDistributionUnderTest() throws Exception {
		TimeSeriesParameter parameter = new TimeSeriesParameter();
		int intervalLength = 4;
		parameter.setIntervalLength(intervalLength);
		int[] spawnsPerInterval = { 1, 0, 0 };
		parameter.setSpawnsPerInterval(spawnsPerInterval);

		PowerMockito.whenNew(MixedDistribution.class).withAnyArguments().thenReturn(distMock);

		MixedParameter paramMock = Mockito.mock(MixedParameter.class);
		PowerMockito.whenNew(MixedParameter.class).withNoArguments().thenReturn(paramMock);

		TimeSeriesDistribution dist = new TimeSeriesDistribution(parameter, spawnNumber, null);

		PowerMockito.verifyNew(MixedDistribution.class).withArguments(paramMock, 1, null);
		PowerMockito.verifyNew(MixedParameter.class).withNoArguments();

		double[] switchpoints = { 4, 8 };
		Mockito.verify(paramMock).setSwitchpoints(switchpoints);

		ObjectMapper mapper = new ObjectMapper();

		MixedParameterDistribution first = new MixedParameterDistribution();
		ConstantParameter constantP = new ConstantParameter();
		constantP.setUpdateFrequency(intervalLength / spawnsPerInterval[0]);
		JsonNode node = mapper.convertValue(constantP, JsonNode.class);
		first.setInterSpawnTimeDistribution("constant");
		first.setDistributionParameters(node);

		MixedParameterDistribution second = new MixedParameterDistribution();
		SingleSpawnParameter singleP1 = new SingleSpawnParameter();
		singleP1.setSpawnNumber(0);
		singleP1.setSpawnTime(8);
		JsonNode node2 = mapper.convertValue(singleP1, JsonNode.class);
		second.setInterSpawnTimeDistribution("singleSpawn");
		second.setDistributionParameters(node2);

		MixedParameterDistribution third = new MixedParameterDistribution();
		SingleSpawnParameter singleP2 = new SingleSpawnParameter();
		singleP2.setSpawnNumber(0);
		singleP2.setSpawnTime(Double.MAX_VALUE);
		JsonNode node3 = mapper.convertValue(singleP2, JsonNode.class);
		third.setInterSpawnTimeDistribution("singleSpawn");
		third.setDistributionParameters(node3);

		ArrayList<MixedParameterDistribution> expectedDistributions = new ArrayList<MixedParameterDistribution>();
		expectedDistributions.add(first);
		expectedDistributions.add(second);
		expectedDistributions.add(third);

		paramMock.setDistributions(expectedDistributions);
		//Mockito.verify(paramMock).setDistributions(expectedDistributions);

		return dist;
	}

	@Override
	public void testGetSpawnNumber() throws Exception {
		Mockito.when(distMock.getSpawnNumber(Mockito.anyDouble())).thenReturn(1);

		VadereDistribution<?> dist = getDistributionUnderTest();
		int actual = dist.getSpawnNumber(1);

		assertEquals(1, actual);
		Mockito.verify(distMock).getSpawnNumber(1d);
	}

	@Override
	public void testRemainingSpawnAgents() throws Exception {
		int expected = 1;
		Mockito.when(distMock.getRemainingSpawnAgents()).thenReturn(expected);

		VadereDistribution<?> dist = getDistributionUnderTest();
		dist.setRemainingSpawnAgents(expected);
		int actual = dist.getRemainingSpawnAgents();

		assertEquals(expected, actual);
		Mockito.verify(distMock).setRemainingSpawnAgents(expected);
		Mockito.verify(distMock).getRemainingSpawnAgents();
	}

	@Override
	public void testGetNextSpawnTime() throws Exception {
		double expected = 1;
		Mockito.when(distMock.getNextSpawnTime(Mockito.anyDouble())).thenReturn(expected);

		VadereDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSpawnTime(1);

		assertEquals(expected, actual, 0);
		Mockito.verify(distMock).getNextSpawnTime(1d);

	}

}
