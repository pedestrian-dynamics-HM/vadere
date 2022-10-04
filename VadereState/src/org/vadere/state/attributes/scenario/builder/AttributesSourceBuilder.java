package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.spawner.AttributesSpawner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AttributesSourceBuilder{
	private ArrayList<Integer> targetIds = new ArrayList<>();
	private ArrayList<Double> groupSizeDistribution = new ArrayList<>(){{add(1.0);}};

	private AttributesVisualElementBuilder visualBuilder = new AttributesVisualElementBuilder();
	private AttributesSpawner spawnerAttributes;

	public AttributesSourceBuilder setVisualBuilder(AttributesVisualElementBuilder visualBuilder) {
		this.visualBuilder = visualBuilder;
		return this;
	}
	public AttributesSourceBuilder setSpawnerAttributes(AttributesSpawner spawnerAttributes) {
		this.spawnerAttributes = spawnerAttributes;
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
		this.targetIds = new ArrayList<>(List.of(targetIds));
		return this;
	}

	public AttributesSourceBuilder setGroupSizeDistribution(ArrayList<Double> groupSizeDistribution) {
		this.groupSizeDistribution = groupSizeDistribution;
		return this;
	}

	public AttributesSource build(AttributesSource attributesSource) {
		attributesSource = (AttributesSource) this.visualBuilder.build(attributesSource);
		attributesSource.setTargetIds(this.targetIds);
		attributesSource.setSpawnerAttributes(spawnerAttributes);
		attributesSource.setGroupSizeDistribution(this.groupSizeDistribution);
		return attributesSource;
	}

	public AttributesSource build(){
		return build(new AttributesSource());
	}
}
