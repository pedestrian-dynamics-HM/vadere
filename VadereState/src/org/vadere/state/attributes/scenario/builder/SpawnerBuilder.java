package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.spawner.VSpawner;

public final class SpawnerBuilder {
    private Integer constraintsElementsMax = 0;

    private Double constraintsTimeStart = 0.0;

    private Double constraintsTimeEnd = 0.0;

    private Boolean eventPositionRandom = false;

    private Boolean eventPositionGridCA= false;

    private Boolean eventPositionFreeSpace= false;
    private AttributesDistribution distribution = new AttributesConstantDistribution();

    private AttributesAgent eventElementAttributes = null;

    private Integer eventElementCount;

    public SpawnerBuilder setConstraintsElementsMax(Integer constraintsElementsMax){
        this.constraintsElementsMax = constraintsElementsMax;
        return this;
    }

    public SpawnerBuilder setConstraintsTimeStart(Double constraintsTimeStart) {
        this.constraintsTimeStart = constraintsTimeStart;
        return this;
    }

    public SpawnerBuilder setConstraintsTimeEnd(Double constraintsTimeEnd) {
        this.constraintsTimeEnd = constraintsTimeEnd;
        return this;
    }

    public SpawnerBuilder setEventPositionRandom(Boolean eventPositionRandom) {
        this.eventPositionRandom = eventPositionRandom;
        return this;
    }

    public SpawnerBuilder setEventPositionGridCA(Boolean eventPositionGridCA) {
        this.eventPositionGridCA = eventPositionGridCA;
        return this;
    }

    public SpawnerBuilder setEventPositionFreeSpace(Boolean eventPositionFreeSpace) {
        this.eventPositionFreeSpace = eventPositionFreeSpace;
        return this;
    }

    public SpawnerBuilder setDistribution(AttributesDistribution distribution) {
        this.distribution = this.distribution;
        return this;
    }

    public AttributesDynamicElement getEventElementAttributes() {
        return eventElementAttributes;
    }

    public SpawnerBuilder setEventElementAttributes(AttributesAgent eventElementAttributes) {
        this.eventElementAttributes = eventElementAttributes;
        return this;
    }

    public Integer getEventElementCount() {
        return eventElementCount;
    }

    public SpawnerBuilder setEventElementCount(Integer eventElementCount) {
        this.eventElementCount = eventElementCount;
        return this;
    }

    public static SpawnerBuilder anAttributesSpawner() {
        try {
            return new SpawnerBuilder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public VSpawner build(VSpawner attributesSpawner){
        var attribs = (AttributesSpawner)attributesSpawner.getAttributes();
        attribs.setConstraintsElementsMax(this.constraintsElementsMax);
        attribs.setConstraintsTimeStart(this.constraintsTimeStart);
        attribs.setConstraintsTimeEnd(this.constraintsTimeEnd);
        attribs.setEventPositionRandom(this.eventPositionRandom);
        attribs.setEventPositionGridCA(this.eventPositionGridCA);
        attribs.setEventPositionFreeSpace(this.eventPositionFreeSpace);
        attribs.setDistributionAttributes(this.distribution);
        attribs.setEventElementAttributes(this.eventElementAttributes);
        //attribs.setEventElementCount(this.eventElementCount);
        attributesSpawner.setAttributes(attribs);
        return attributesSpawner;
    }

}
