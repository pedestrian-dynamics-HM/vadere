package org.vadere.state.attributes.scenario;

import org.junit.Test;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TestAttributesSource {

	private AttributesSource attributes;

	private void createAttributes(SourceTestAttributesBuilder builder) throws IOException {
		attributes = builder.getResult();
	}

	@Test
	public void testGetInterSpawnTimeDistribution() throws Exception {
		createAttributes(new SourceTestAttributesBuilder());
		assertTrue(AttributesConstantDistribution.class.isAssignableFrom(attributes.getSpawnerAttributes().getDistributionAttributes().getClass()));
	}

	// TODO
//	@Test
//	public void testGetDistributionParameters() throws IOException {
//		final SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
//				.setDistributionParameters(new double[] {1, 2, 3});
//		createAttributes(builder);
//		List<Double> expected = new ArrayList<>(3);
//		expected.add(1.0);
//		expected.add(2.0);
//		expected.add(3.0);
//		assertEquals(expected, attributes.getDistributionParameters());
//	}

}
