package org.vadere.state.scenario.distribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */

/*

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DistributionFactory.class, DistributionRegistry.class })
public class DistributionFactoryTest {

	@Test
	public void testCreate() throws Exception {
		// Prepare Mocks
		ObjectMapper mapperMock = Mockito.mock(ObjectMapper.class);
		PowerMockito.whenNew(ObjectMapper.class).withNoArguments().thenReturn(mapperMock);

		RegisteredDistribution regDist = Mockito.mock(RegisteredDistribution.class);
		Mockito.doReturn(Object.class).when(regDist).getParameter();
		Mockito.doReturn(DistributionStub.class).when(regDist).getDistribution();

		PowerMockito.mockStatic(DistributionRegistry.class);
		String name = "";
		Mockito.when(DistributionRegistry.get(name)).thenReturn(regDist);

		AttributesConstantDistribution parametersMock = Mockito.mock(AttributesConstantDistribution.class);

		// Call
		VDistribution<?> actual = DistributionFactory.create(parametersMock,  null);

		// Verify
		PowerMockito.verifyStatic(DistributionRegistry.class);
		DistributionRegistry.get(name);

		Mockito.verify(regDist).getDistribution();
		Mockito.verify(regDist).getParameter();

		Mockito.verify(mapperMock).readValue(parametersMock.toString(), Object.class);

		assertTrue(actual instanceof DistributionStub);
	}

}*/
