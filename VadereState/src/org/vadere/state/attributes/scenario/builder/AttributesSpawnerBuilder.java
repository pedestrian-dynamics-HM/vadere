package org.vadere.state.attributes.scenario.builder;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.spawner.*;
import org.vadere.state.scenario.spawner.VSpawner;
import org.vadere.state.scenario.spawner.impl.LERPSpawner;
import org.vadere.state.scenario.spawner.impl.MixedSpawner;
import org.vadere.state.scenario.spawner.impl.RegularSpawner;
import org.vadere.state.scenario.spawner.impl.TimeSeriesSpawner;
import org.vadere.state.util.Views;
import org.vadere.util.reflection.VadereAttribute;

public class AttributesSpawnerBuilder {

    protected Integer constraintsElementsMax = 0;

    protected Double constraintsTimeStart = 0.0;

    protected Double constraintsTimeEnd = 0.0;

    protected Boolean eventPositionRandom = false;

    protected Boolean eventPositionGridCA = false;

    protected Boolean eventPositionFreeSpace = false;

    protected Integer eventElementCount = 0;

    protected AttributesAgent eventElement = null;

    public AttributesSpawnerBuilder setConstraintsElementsMax(Integer constraintsElementsMax) {
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

    public AttributesSpawnerBuilder setEventElementCount(Integer eventElementCount) {
        this.eventElementCount = eventElementCount;
        return this;
    }

    public AttributesSpawnerBuilder setEventElement(AttributesAgent eventElement) {
        this.eventElement = eventElement;
        return this;
    }

    public AttributesSpawner build(Class<? extends VSpawner> spawnerClass) {
        AttributesSpawner attributes = null;
        if(spawnerClass.isAssignableFrom(RegularSpawner.class)){
            attributes = new AttributesRegularSpawner();
        }
        if(spawnerClass.isAssignableFrom(LERPSpawner.class)){
            attributes = new AttributesLerpSpawner();
        }
        if(spawnerClass.isAssignableFrom(MixedSpawner.class)){
            attributes = new AttributesMixedSpawner();
        }
        if(spawnerClass.isAssignableFrom(TimeSeriesSpawner.class)){
            attributes = new AttributesTimeSeriesSpawner();
        }
        attributes.setConstraintsElementsMax(this.constraintsElementsMax);
        attributes.setConstraintsTimeStart(this.constraintsTimeStart);
        attributes.setConstraintsTimeEnd(this.constraintsTimeEnd);
        attributes.setEventPositionRandom(this.eventPositionRandom);
        attributes.setEventPositionGridCA(this.eventPositionGridCA);
        attributes.setEventPositionFreeSpace(this.eventPositionFreeSpace);
        attributes.setEventElementCount(this.eventElementCount);
        attributes.setEventElementAttributes(this.eventElement);
        return  attributes;
    }
}
