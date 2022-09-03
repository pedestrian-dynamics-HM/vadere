package org.vadere.state.attributes.spawner;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.scenario.AttributesVisualElement;
import org.vadere.state.util.Views;
import org.vadere.util.Attributes;
import org.vadere.util.reflection.VadereAttribute;

public abstract class AttributesSpawner extends Attributes {
    public static final int NO_MAX_SPAWN_NUMBER_TOTAL = -1;
    public static final String CONSTANT_DISTRIBUTION = "org.vadere.state.scenario.distribution.impl.ConstantDistribution";
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected Integer constraintsElementsMax;
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected Double constraintsTimeStart;
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected Double constraintsTimeEnd;
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected Boolean eventPositionRandom;
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected Boolean eventPositionGridCA;
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected Boolean eventPositionFreeSpace;
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected AttributesVisualElement eventElement;
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected AttributesDistribution distribution;

    public AttributesSpawner(){}
    public Integer getConstraintsElementsMax() {
        return constraintsElementsMax;
    }

    public void setConstraintsElementsMax(Integer constraintsElementsMax) {
        this.constraintsElementsMax = constraintsElementsMax;
    }

    public Double getConstraintsTimeStart() {
        return constraintsTimeStart;
    }

    public void setConstraintsTimeStart(Double constraintsTimeStart) {
        checkSealed();
        this.constraintsTimeStart = constraintsTimeStart;
    }

    public Double getConstraintsTimeEnd() {
        return constraintsTimeEnd;
    }

    public void setConstraintsTimeEnd(Double constraintsTimeEnd) {
        checkSealed();
        this.constraintsTimeEnd = constraintsTimeEnd;
    }

    public Boolean isEventPositionRandom() {
        return eventPositionRandom;
    }

    public void setEventPositionRandom(Boolean eventPositionRandom) {
        checkSealed();
        this.eventPositionRandom = eventPositionRandom;
    }

    public Boolean isEventPositionGridCA() {
        return eventPositionGridCA;
    }

    public void setEventPositionGridCA(Boolean eventPositionGridCA) {
        checkSealed();
        this.eventPositionGridCA = eventPositionGridCA;
    }

    public Boolean isEventPositionFreeSpace() {
        return eventPositionFreeSpace;
    }

    public void setEventPositionFreeSpace(Boolean eventPositionFreeSpace) {
        checkSealed();
        this.eventPositionFreeSpace = eventPositionFreeSpace;
    }

    public AttributesVisualElement getEventElementAttributes() {
        return eventElement;
    }

    public void setEventElementAttributes(AttributesVisualElement eventElement) {
        checkSealed();
        this.eventElement = eventElement;
    }

    public AttributesDistribution getDistributionAttributes() {
        return distribution;
    }

    public void setDistributionAttributes(AttributesDistribution distribution) {
        checkSealed();
        this.distribution = distribution;
    }

    public abstract int getEventElementCount();

    public abstract void setEventElementCount(Integer eventElementCount);
}
