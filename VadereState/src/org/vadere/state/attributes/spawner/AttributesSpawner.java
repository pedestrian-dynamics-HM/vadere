package org.vadere.state.attributes.spawner;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesVisualElement;
import org.vadere.state.util.Views;
import org.vadere.util.reflection.VadereAttribute;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AttributesRegularSpawner.class, name = "org.vadere.state.attributes.spawner.AttributesRegularSpawner"),
        @JsonSubTypes.Type(value = AttributesLerpSpawner.class, name = "org.vadere.state.attributes.spawner.AttributesLerpSpawner"),
        @JsonSubTypes.Type(value = AttributesMixedSpawner.class, name = "org.vadere.state.attributes.spawner.AttributesMixedSpawner"),
        @JsonSubTypes.Type(value = AttributesTimeSeriesSpawner.class, name = "org.vadere.state.attributes.spawner.AttributesTimeSeriesSpawner")
})
public abstract class AttributesSpawner extends Attributes {
    @VadereAttribute(exclude = true)
    public static final int NO_MAX_SPAWN_NUMBER_TOTAL = -1;
    @VadereAttribute(exclude = true)
    public static final String CONSTANT_DISTRIBUTION = "org.vadere.state.scenario.distribution.impl.ConstantDistribution";
    /**
     * This attribute stores the maximum number of agents that can be spawned by this spawner. No limit, if set to -1.
     */
    @JsonView(Views.CacheViewExclude.class)
    protected Integer constraintsElementsMax = NO_MAX_SPAWN_NUMBER_TOTAL;
    /**
     * This attribute stores the starting time of a spawner after which agents can be spawned.
     */
    @VadereAttribute(group = "Time Constraints")
    @JsonView(Views.CacheViewExclude.class)
    protected Double constraintsTimeStart = 0.0;
    /**
     * This attribute stores the ending time of a spawner after which no agents can be spawned anymore.
     */
    @VadereAttribute(group = "Time Constraints")
    @JsonView(Views.CacheViewExclude.class)
    protected Double constraintsTimeEnd = 0.0;
    @VadereAttribute(group = "Position Constraints")
    @JsonView(Views.CacheViewExclude.class)
    protected Boolean eventPositionRandom = false;
    @VadereAttribute(group = "Position Constraints")
    @JsonView(Views.CacheViewExclude.class)
    protected Boolean eventPositionGridCA = false;
    @VadereAttribute(group = "Position Constraints")
    @JsonView(Views.CacheViewExclude.class)
    protected Boolean eventPositionFreeSpace = true;
    /**
     * This attribute stores the number of agents that can be spawned maximum in every spawn event
     * triggered by the distribution.
     */
    @VadereAttribute
    protected Integer eventElementCount = 1;

    @VadereAttribute(exclude = true)
    @JsonView(Views.CacheViewExclude.class)
    protected AttributesAgent eventElement = null;



    public AttributesSpawner(){
    }

    public AttributesSpawner(AttributesDistribution distribution){
        setDistributionAttributes(distribution);
    }
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

    public void setEventElementAttributes(AttributesAgent eventElement) {
        checkSealed();
        this.eventElement = eventElement;
    }

    public void setEventElementCount(Integer eventElementCount){
        checkSealed();
        this.eventElementCount = eventElementCount;
    }
    public Integer getEventElementCount() {
        return eventElementCount;
    }

    public abstract AttributesDistribution getDistributionAttributes();

    public abstract void setDistributionAttributes(AttributesDistribution distribution);

}
