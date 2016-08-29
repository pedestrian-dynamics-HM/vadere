package org.vadere.state.attributes.scenario;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.vadere.state.scenario.ConstantDistribution;
import org.vadere.util.io.IOUtils;

public class TestAttributesSource {

	private AttributesSource attributes;

	private static String generateSourceAttributesJson(double startTime, double endTime,
			int spawnNumber, double spawnDelay, String distributionParams, boolean useFreeSpaceOnly) {
		return "{\"shape\": {\"type\": \"POLYGON\",\"points\": ["
				+ "{\"x\": 0.0,\"y\": 0.0},{\"x\": 0.1,\"y\": 0},{\"x\": 0.1,\"y\": 0.1},{\"x\": 0,\"y\": 0.1}]},"
				+ "\"spawnDelay\": " + spawnDelay
				+ ",\"spawnNumber\":  " + spawnNumber
				+ ",\"interSpawnTimeDistribution\": \"" + AttributesSource.CONSTANT_DISTRIBUTION + "\""
				+ ",\"distributionParameters\": [" + distributionParams + "]"
				+ ",\"startTime\": " + startTime
				+ ",\"endTime\": " + endTime
				+ ",\"spawnAtRandomPositions\": true"
				+ ",\"useFreeSpaceOnly\": " + useFreeSpaceOnly
				+ ",\"targetIds\": [1]}";
	}

	private void createAttributes(double startTime, double endTime, int spawnNumber,
			double spawnDelay, String distributionParams, boolean useFreeSpaceOnly) {
		attributes = IOUtils.getGson().fromJson(
				generateSourceAttributesJson(startTime, endTime, spawnNumber, spawnDelay, distributionParams,
						useFreeSpaceOnly),
				AttributesSource.class);
	}

	@Test
	public void testGetInterSpawnTimeDistribution() {
		createAttributes(0, 0, 0, 0, "", false);
		assertEquals(ConstantDistribution.class.getName(), attributes.getInterSpawnTimeDistribution());
	}

	@Test
	public void testGetDistributionParameters() {
		createAttributes(0, 0, 0, 0, "1, 2, 3", false);
		List<Double> expected = new ArrayList<>(3);
		expected.add(1.0);
		expected.add(2.0);
		expected.add(3.0);
		assertEquals(expected, attributes.getDistributionParameters());
	}

}
