package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.scenario.AttributesSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AttributesSourceBuilder{
	private ArrayList<Integer> targetIds = new ArrayList<>();
	private ArrayList<Double> groupSizeDistribution = (ArrayList<Double>) List.of(1.0);

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


	public AttributesSourceBuilder setTargetIds(ArrayList<Integer> targetIds) {
		this.targetIds = targetIds;
		return this;
	}

	public AttributesSourceBuilder setTargetIds(Integer... targetIds) {
		this.targetIds = (ArrayList<Integer>) Arrays.asList(targetIds);
		return this;
	}

	public AttributesSourceBuilder setGroupSizeDistribution(ArrayList<Double> groupSizeDistribution) {
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
