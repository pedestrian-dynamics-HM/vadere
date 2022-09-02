package org.vadere.state.attributes.scenario.builder;

import org.vadere.util.Attributes;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.impl.ConstantDistribution;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.types.DynamicElementType;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class AttributesSourceBuilder {
	private VShape shape = null;
	private VDistribution interSpawnTimeDistribution;
	private AttributesDistribution distributionParameters = new AttributesConstantDistribution(0.0);
	private int spawnNumber = 1;
	private int maxSpawnNumberTotal = AttributesSource.NO_MAX_SPAWN_NUMBER_TOTAL;
	private double startTime = 0;
	private double endTime = 0;
	private boolean spawnAtRandomPositions;
	private boolean useFreeSpaceOnly = true;
	private List<Integer> targetIds = new LinkedList<>();
	private List<Double> groupSizeDistribution = Arrays.asList(1.0);
	private DynamicElementType dynamicElementType = DynamicElementType.PEDESTRIAN;
	private int id = Attributes.ID_NOT_SET;

	private AttributesSourceBuilder() throws Exception {
		this.interSpawnTimeDistribution = new ConstantDistribution((AttributesConstantDistribution) distributionParameters,spawnNumber, null);
	}

	public static AttributesSourceBuilder anAttributesSource() {
		try {
			return new AttributesSourceBuilder();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public AttributesSourceBuilder shape(VShape shape) {
		this.shape = shape;
		return this;
	}

	public AttributesSourceBuilder interSpawnTimeDistribution(VDistribution interSpawnTimeDistribution) {
		this.interSpawnTimeDistribution = interSpawnTimeDistribution;
		return this;
	}

	public AttributesSourceBuilder distributionParameters(AttributesDistribution distributionParameters) {
		this.distributionParameters = distributionParameters;
		return this;
	}

	public AttributesSourceBuilder spawnNumber(int spawnNumber) {
		this.spawnNumber = spawnNumber;
		return this;
	}

	public AttributesSourceBuilder maxSpawnNumberTotal(int maxSpawnNumberTotal) {
		this.maxSpawnNumberTotal = maxSpawnNumberTotal;
		return this;
	}

	public AttributesSourceBuilder startTime(double startTime) {
		this.startTime = startTime;
		return this;
	}

	public AttributesSourceBuilder endTime(double endTime) {
		this.endTime = endTime;
		return this;
	}

	public AttributesSourceBuilder spawnAtRandomPositions(boolean spawnAtRandomPositions) {
		this.spawnAtRandomPositions = spawnAtRandomPositions;
		return this;
	}

	public AttributesSourceBuilder useFreeSpaceOnly(boolean useFreeSpaceOnly) {
		this.useFreeSpaceOnly = useFreeSpaceOnly;
		return this;
	}

	public AttributesSourceBuilder targetIds(List<Integer> targetIds) {
		this.targetIds = targetIds;
		return this;
	}

	public AttributesSourceBuilder targetIds(Integer... targetIds) {
		this.targetIds = Arrays.asList(targetIds);
		return this;
	}

	public AttributesSourceBuilder groupSizeDistribution(List<Double> groupSizeDistribution) {
		this.groupSizeDistribution = groupSizeDistribution;
		return this;
	}

	public AttributesSourceBuilder dynamicElementType(DynamicElementType dynamicElementType) {
		this.dynamicElementType = dynamicElementType;
		return this;
	}

	public AttributesSourceBuilder id(int id) {
		this.id = id;
		return this;
	}

	public AttributesSource build() {
		AttributesSource attributesSource = new AttributesSource(id);
		attributesSource.setShape(shape);
		attributesSource.setInterSpawnTimeDistribution(interSpawnTimeDistribution);
		attributesSource.setDistributionParameters(distributionParameters);
		attributesSource.setSpawnNumber(spawnNumber);
		attributesSource.setMaxSpawnNumberTotal(maxSpawnNumberTotal);
		attributesSource.setStartTime(startTime);
		attributesSource.setEndTime(endTime);
		attributesSource.setSpawnAtRandomPositions(spawnAtRandomPositions);
		attributesSource.setUseFreeSpaceOnly(useFreeSpaceOnly);
		attributesSource.setTargetIds(targetIds);
		attributesSource.setGroupSizeDistribution(groupSizeDistribution);
		attributesSource.setDynamicElementType(dynamicElementType);
		return attributesSource;
	}
}
