package org.vadere.state.scenario.distribution.registry;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertTrue;

public class DistributionRegistryTest {

	@Test
	public void testExpectedDistributionsRegistered() {
		Set<String> expected = ImmutableSet.of(
				"org.vadere.state.attributes.distributions.AttributesBinomialDistribution",
				"org.vadere.state.attributes.distributions.AttributesConstantDistribution",
				"org.vadere.state.attributes.distributions.AttributesNormalDistribution",
				"org.vadere.state.attributes.distributions.AttributesEmpiricalDistribution",
				"org.vadere.state.attributes.distributions.AttributesLinearInterpolationDistribution",
		        "org.vadere.state.attributes.distributions.AttributesMixedDistribution",
				"org.vadere.state.attributes.distributions.AttributesNegativeExponentialDistribution",
				"org.vadere.state.attributes.distributions.AttributesNormalDistribution",
				"org.vadere.state.attributes.distributions.AttributesPoissonDistribution",
				"org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution",
				"org.vadere.state.attributes.distributions.AttributesTimeSeriesDistribution"
		);

		Set<String> actual = DistributionRegistry.getRegisteredNames();

		assertTrue(actual.containsAll(expected));
	}

	@Test
	public void testGetWithExistingDist() throws Exception {
		DistributionRegistry.get("org.vadere.state.attributes.distributions.AttributesNormalDistribution");

	}

	@Test(expected = Exception.class)
	public void testGetWithNotExistingDist() throws Exception {
		DistributionRegistry.get("some random text");
	}

}
