package org.vadere.state.scenario.distribution.impl;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */

/*
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TimeSeriesDistribution.class })
public class TimeSeriesDistributionTest extends VadereDistributionTest {

	@Mock
	MixedDistribution distMock;

	@Override
	protected VDistribution<?> getDistributionUnderTest() throws Exception {
		AttributesTimeSeriesDistribution parameter = new AttributesTimeSeriesDistribution();
		int intervalLength = 4;
		parameter.setIntervalLength(intervalLength);
		ArrayList<Integer> spawnsPerInterval =  new ArrayList<>(){{ new Integer(1);new Integer(0);new Integer(0); }};
		parameter.setSpawnsPerInterval(spawnsPerInterval);

		PowerMockito.whenNew(MixedDistribution.class).withAnyArguments().thenReturn(distMock);

		AttributesMixedDistribution paramMock = Mockito.mock(AttributesMixedDistribution.class);
		PowerMockito.whenNew(AttributesMixedDistribution.class).withNoArguments().thenReturn(paramMock);

		TimeSeriesDistribution dist = new TimeSeriesDistribution(parameter,null);

		PowerMockito.verifyNew(MixedDistribution.class).withArguments(paramMock, null);
		PowerMockito.verifyNew(AttributesMixedDistribution.class).withNoArguments();

		ArrayList<Double> switchpoints = new ArrayList<>(){{ new Double(4); new Double(8); }};
		Mockito.verify(paramMock).setSwitchpoints(switchpoints);

		AttributesConstantDistribution constantP = new AttributesConstantDistribution();
		constantP.setUpdateFrequency(intervalLength / spawnsPerInterval.get(0));

		AttributesSingleSpawnDistribution singleP1 = new AttributesSingleSpawnDistribution();
		singleP1.setSpawnTime(8);

		AttributesSingleSpawnDistribution  singleP2 = new AttributesSingleSpawnDistribution ();
		singleP2.setSpawnTime(Double.MAX_VALUE);

		ArrayList<AttributesDistribution> expectedDistributions = new ArrayList<>();
		expectedDistributions.add(constantP);
		expectedDistributions.add(singleP1);
		expectedDistributions.add(singleP2 );

		paramMock.setDistributions(expectedDistributions);
		//Mockito.verify(paramMock).setDistributions(expectedDistributions);

		return dist;
	}
	@Override
	public void testGetNextSample() throws Exception {
		double expected = 1;
		Mockito.when(distMock.getNextSample(Mockito.anyDouble())).thenReturn(expected);

		VDistribution<?> dist = getDistributionUnderTest();
		double actual = dist.getNextSample(1);

		assertEquals(expected, actual, 0);
		Mockito.verify(distMock).getNextSample(1d);

	}

}*/