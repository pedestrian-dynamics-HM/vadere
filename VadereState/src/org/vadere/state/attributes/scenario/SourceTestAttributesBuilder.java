package org.vadere.state.attributes.scenario;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math3.distribution.RealDistribution;
import org.vadere.state.scenario.ConstantDistribution;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.state.util.TextOutOfNodeException;
import org.vadere.util.io.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

public class SourceTestAttributesBuilder {

	private double startTime = 1;
	private double endTime = 2;
	private int spawnNumber = 1;
	private boolean useFreeSpaceOnly = false;
	private Class<? extends RealDistribution> distributionClass = ConstantDistribution.class;
	private double[] distributionParams = new double[] { 1 };
	private int maxSpawnNumberTotal = AttributesSource.NO_MAX_SPAWN_NUMBER_TOTAL;
	private double x0 = 0.1;
	private double y0 = 0;
	private double x1 = 0.1;
	private double y1 = 0.1;
	private double x2 = 0;
	private double y2 = 0.1;

	public AttributesSource getResult() {
		String json = generateSourceAttributesJson();
		return StateJsonConverter.deserializeObjectFromJson(json, AttributesSource.class);
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

	public SourceTestAttributesBuilder setDistributionParameters(double[] params) {
		distributionParams = params;
		return this;
	}

	public SourceTestAttributesBuilder setSourceDim(double width, double height) {
		x0 = width;
		y0 = 0;
		x1 = width;
		y1 = height;
		x2 = 0;
		y2 = height;
		return this;
	}


	private String generateSourceAttributesJson() {
		return "{\"shape\": {\"type\": \"POLYGON\",\"points\":"
				+ "[{\"x\": 0.0,\"y\": 0.0}"
				+ ",{\"x\": " + x0 + ",\"y\": " + y0 + "}"
				+ ",{\"x\": " + x1 + ",\"y\": " + y1 + "}"
				+ ",{\"x\": " + x2 + ",\"y\": " + y2 + "}]}"
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
