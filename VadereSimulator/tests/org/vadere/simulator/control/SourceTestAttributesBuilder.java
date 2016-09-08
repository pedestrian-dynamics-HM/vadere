package org.vadere.simulator.control;

import java.util.Arrays;

import org.apache.commons.math3.distribution.RealDistribution;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.ConstantDistribution;
import org.vadere.util.io.IOUtils;

public class SourceTestAttributesBuilder {

	private double startTime = 1;
	private double endTime = 2;
	private int spawnNumber = 1;
	private boolean useFreeSpaceOnly = false;
	private Class<? extends RealDistribution> distributionClass = ConstantDistribution.class;
	private double[] distributionParams = new double[] { 1 };
	private int maxSpawnNumberTotal = AttributesSource.NO_MAX_SPAWN_NUMBER_TOTAL;
	
	public AttributesSource getResult() {
		String json = generateSourceAttributesJson();
		return IOUtils.getGson().fromJson(json, AttributesSource.class);
	}

	public SourceTestAttributesBuilder setOneTimeSpawn(double time) {
		this.startTime = time;
		this.endTime = time;
		return this;
	}

	public SourceTestAttributesBuilder setStartTime(double startTime) {
		this.startTime = startTime;
		return this;
	}

	public SourceTestAttributesBuilder setEndTime(double endTime) {
		this.endTime = endTime;
		return this;
	}

	public SourceTestAttributesBuilder setSpawnNumber(int spawnNumber) {
		this.spawnNumber = spawnNumber;
		return this;
	}

	public SourceTestAttributesBuilder setSpawnIntervalForConstantDistribution(double spawnDelay) {
		this.distributionParams = new double[] {spawnDelay};
		return this;
	}

	public SourceTestAttributesBuilder setUseFreeSpaceOnly(boolean useFreeSpaceOnly) {
		this.useFreeSpaceOnly = useFreeSpaceOnly;
		return this;
	}

	public SourceTestAttributesBuilder setDistributionClass(Class<? extends RealDistribution> distributionClass) {
		this.distributionClass = distributionClass;
		return this;
	}
	
	public SourceTestAttributesBuilder setMaxSpawnNumberTotal(int maxSpawnNumberTotal) {
		this.maxSpawnNumberTotal = maxSpawnNumberTotal;
		return this;
	}

	private String generateSourceAttributesJson() {
		return "{\"shape\": {\"type\": \"POLYGON\",\"points\":"
				+ "[{\"x\": 0.0,\"y\": 0.0}"
				+ ",{\"x\": 0.1,\"y\": 0}"
				+ ",{\"x\": 0.1,\"y\": 0.1}"
				+ ",{\"x\": 0,\"y\": 0.1}]}"
				+ ",\"spawnNumber\":  " + spawnNumber
				+ ",\"maxSpawnNumberTotal\":  " + maxSpawnNumberTotal
				+ ",\"interSpawnTimeDistribution\": \"" + distributionClass.getName() + "\""
				+ ",\"distributionParameters\": " + Arrays.toString(distributionParams)
				+ ",\"startTime\": " + startTime
				+ ",\"endTime\": " + endTime
				+ ",\"spawnAtRandomPositions\": true"
				+ ",\"useFreeSpaceOnly\": " + useFreeSpaceOnly
				+ ",\"targetIds\": [1]}";
	}
}
