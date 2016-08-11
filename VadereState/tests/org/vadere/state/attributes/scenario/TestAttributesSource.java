package org.vadere.state.attributes.scenario;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesSource;
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

	@SuppressWarnings("deprecation")
	@Test
	public void testGetSpawnDelayWhenDeprecatedSpawnDelayIsUndefined() {
		createAttributes(0, 0, 0, -1, "1", false);
		assertEquals(1, attributes.getSpawnDelay(), 0.00001);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testGetSpawnDelayWhenDeprecatedSpawnDelayNotUndefined() {
		createAttributes(0, 0, 0, 1, "", false);
		assertEquals(1, attributes.getSpawnDelay(), 0.00001);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IndexOutOfBoundsException.class)
	public void testGetSpawnDelayWhenUndefined() {
		// spawnDelay == -1 and distributionParams == []
		createAttributes(0, 0, 0, -1, "", false);
		attributes.getSpawnDelay();
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
