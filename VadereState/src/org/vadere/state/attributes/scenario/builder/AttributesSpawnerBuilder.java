package org.vadere.state.attributes.scenario.builder;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.util.Views;
import org.vadere.util.reflection.VadereAttribute;

public final class AttributesSpawnerBuilder {
    private Integer constraintsElementsMax = 0;

    private Double constraintsTimeStart = 0.0;

    private Double constraintsTimeEnd = 0.0;

    private Boolean eventPositionRandom = false;

    private Boolean eventPositionGridCA= false;

    private Boolean eventPositionFreeSpace= false;
    private AttributesDistribution distributionAttributes = new AttributesConstantDistribution();

    private AttributesDynamicElement eventElementAttributes = null;

    private Integer eventElementCount;

    public AttributesSpawnerBuilder setConstraintsElementsMax(Integer constraintsElementsMax){
        this.constraintsElementsMax = constraintsElementsMax;
        return this;
    }

    public AttributesSpawnerBuilder setConstraintsTimeStart(Double constraintsTimeStart) {
        this.constraintsTimeStart = constraintsTimeStart;
        return this;
    }

    public AttributesSpawnerBuilder setConstraintsTimeEnd(Double constraintsTimeEnd) {
        this.constraintsTimeEnd = constraintsTimeEnd;
        return this;
    }

    public AttributesSpawnerBuilder setEventPositionRandom(Boolean eventPositionRandom) {
        this.eventPositionRandom = eventPositionRandom;
        return this;
    }

    public AttributesSpawnerBuilder setEventPositionGridCA(Boolean eventPositionGridCA) {
        this.eventPositionGridCA = eventPositionGridCA;
        return this;
    }

    public AttributesSpawnerBuilder setEventPositionFreeSpace(Boolean eventPositionFreeSpace) {
        this.eventPositionFreeSpace = eventPositionFreeSpace;
        return this;
    }

    public AttributesSpawnerBuilder setDistributionAttributes(AttributesDistribution distributionAttributes) {
        this.distributionAttributes = distributionAttributes;
        return this;
    }

    public AttributesDynamicElement getEventElementAttributes() {
        return eventElementAttributes;
    }

    public AttributesSpawnerBuilder setEventElementAttributes(AttributesDynamicElement eventElementAttributes) {
        this.eventElementAttributes = eventElementAttributes;
        return this;
    }

    public Integer getEventElementCount() {
        return eventElementCount;
    }

    public AttributesSpawnerBuilder setEventElementCount(Integer eventElementCount) {
        this.eventElementCount = eventElementCount;
        return this;
    }

    public static AttributesSpawnerBuilder anAttributesSpawner() {
        try {
            return new AttributesSpawnerBuilder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AttributesSpawner build(AttributesSpawner attributesSpawner){
        attributesSpawner.setConstraintsElementsMax(this.constraintsElementsMax);
        attributesSpawner.setConstraintsTimeStart(this.constraintsTimeStart);
        attributesSpawner.setConstraintsTimeEnd(this.constraintsTimeEnd);
        attributesSpawner.setEventPositionRandom(this.eventPositionRandom);
        attributesSpawner.setEventPositionGridCA(this.eventPositionGridCA);
        attributesSpawner.setEventPositionFreeSpace(this.eventPositionFreeSpace);
        attributesSpawner.setDistributionAttributes(this.distributionAttributes);
        attributesSpawner.setEventElementAttributes(this.eventElementAttributes);
        attributesSpawner.setEventElementCount(this.eventElementCount);
        return attributesSpawner;
    }
}
