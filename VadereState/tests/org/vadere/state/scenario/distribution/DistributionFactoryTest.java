package org.vadere.state.scenario.distribution;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vadere.state.scenario.distribution.registry.DistributionRegistry;
import org.vadere.state.scenario.distribution.registry.RegisteredDistribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
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

		JsonNode parametersMock = Mockito.mock(JsonNode.class);

		// Call
		VadereDistribution<?> actual = DistributionFactory.create(name, parametersMock, 1, null);

		// Verify
		PowerMockito.verifyNew(ObjectMapper.class).withNoArguments();

		PowerMockito.verifyStatic(DistributionRegistry.class);
		DistributionRegistry.get(name);

		Mockito.verify(regDist).getDistribution();
		Mockito.verify(regDist).getParameter();

		Mockito.verify(mapperMock).readValue(parametersMock.toString(), Object.class);

		assertTrue(actual instanceof DistributionStub);
	}

}
