package org.vadere.state.scenario.distribution.registry;

import java.util.Set;
import org.junit.Test;
import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.*;

public class DistributionRegistryTest {

	@Test
	public void testExpectedDistributionsRegistered() {
		Set<String> expected = ImmutableSet.of("negativeExponential", "normal", "linearInterpolation", "constant",
		        "empirical", "poisson", "singleSpawn");

		Set<String> actual = DistributionRegistry.getRegisteredNames();

		assertTrue(actual.containsAll(expected));
	}

	@Test
	public void testGetWithExistingDist() throws Exception {
		DistributionRegistry.get("normal");

	}

	@Test(expected = Exception.class)
	public void testGetWithNotExistingDist() throws Exception {
		DistributionRegistry.get("some random text");
	}

}
