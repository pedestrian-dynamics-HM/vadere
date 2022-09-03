package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.spawner.AttributesRegularSpawner;
import org.vadere.state.attributes.spawner.AttributesSpawner;
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

public final class AttributesSourceBuilder{
	private List<Integer> targetIds = new LinkedList<>();
	private List<Double> groupSizeDistribution = Arrays.asList(1.0);

	private AttributesVisualElementBuilder visualBuilder = new AttributesVisualElementBuilder();
	private AttributesSpawnerBuilder spawnerBuilder;

	public AttributesSourceBuilder setVisualBuilder(AttributesVisualElementBuilder visualBuilder) {
		this.visualBuilder = visualBuilder;
		return this;
	}
	public AttributesSourceBuilder setSpawnerBuilder(AttributesSpawnerBuilder spawnerBuilder) {
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

	public AttributesSource build(AttributesSource attributesSource,AttributesSpawner attributesSpawner) {
		attributesSource = (AttributesSource) this.visualBuilder.build(attributesSource);
		attributesSource.setSpawnerAttributes(this.spawnerBuilder.build(attributesSpawner));
		attributesSource.setTargetIds(this.targetIds);
		attributesSource.setGroupSizeDistribution(this.groupSizeDistribution);
		return attributesSource;
	}

	public AttributesSource buildWithRegularSpawner(AttributesSource attributesSource) {
		build(attributesSource,new AttributesRegularSpawner());
		return attributesSource;
	}
	public AttributesSource build(){
		return buildWithRegularSpawner(new AttributesSource());
	}
}
