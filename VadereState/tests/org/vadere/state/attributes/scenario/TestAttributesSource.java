package org.vadere.state.attributes.scenario;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

public class TestAttributesSource {

	private AttributesSource attributes;

	private void createAttributes(SourceTestAttributesBuilder builder) throws IOException {
		attributes = builder.getResult();
	}

	@Test
	public void testGetInterSpawnTimeDistribution() throws Exception {
		createAttributes(new SourceTestAttributesBuilder());
		assertEquals("constant", attributes.getInterSpawnTimeDistribution());
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
