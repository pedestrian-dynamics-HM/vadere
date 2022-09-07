package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.scenario.AttributesSource;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class AttributesSourceBuilder{
	private List<Integer> targetIds = new LinkedList<>();
	private List<Double> groupSizeDistribution = List.of(1.0);

	private AttributesVisualElementBuilder visualBuilder = new AttributesVisualElementBuilder();
	private SpawnerBuilder spawnerBuilder;

	public AttributesSourceBuilder setVisualBuilder(AttributesVisualElementBuilder visualBuilder) {
		this.visualBuilder = visualBuilder;
		return this;
	}
	public AttributesSourceBuilder setSpawnerBuilder(SpawnerBuilder spawnerBuilder) {
		this.spawnerBuilder = spawnerBuilder;
		return this;
	}


	public static AttributesSourceBuilder anAttributesSource() {
		try {
			return new AttributesSourceBuilder();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	public AttributesSourceBuilder setTargetIds(List<Integer> targetIds) {
		this.targetIds = targetIds;
		return this;
	}

	public AttributesSourceBuilder setTargetIds(Integer... targetIds) {
		this.targetIds = Arrays.asList(targetIds);
		return this;
	}

	public AttributesSourceBuilder setGroupSizeDistribution(List<Double> groupSizeDistribution) {
		this.groupSizeDistribution = groupSizeDistribution;
		return this;
	}

	public AttributesSource build(AttributesSource attributesSource) {
		attributesSource = (AttributesSource) this.visualBuilder.build(attributesSource);
		attributesSource.setTargetIds(this.targetIds);
		attributesSource.setGroupSizeDistribution(this.groupSizeDistribution);
		return attributesSource;
	}
}
