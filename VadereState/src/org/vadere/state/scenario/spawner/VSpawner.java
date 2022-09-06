package org.vadere.state.scenario.spawner;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.vadere.state.attributes.distributions.AttributesBinomialDistribution;
import org.vadere.state.scenario.spawner.impl.RegularSpawner;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.Topography;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.distribution.VDistribution;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegularSpawner.class, name = "org.vadere.state.scenario.spawner.impl.RegularSpawner"),
})
public abstract class VSpawner  implements AttributesAttached {
    private final Topography topography = null;
    protected AttributesSpawner spawnerAttributes;
    protected int dynamicElementsCreatedTotal = 0;

    public VSpawner(){}

    public VSpawner(AttributesSpawner attributes) {
        this.spawnerAttributes = attributes;
    }

    public abstract int getSpawnNumber(double timeCurrentEvent);

    public abstract int getRemainingSpawnAgents();

    public abstract void update(double simTimeInSec);

    public abstract void setRemainingSpawnAgents(int remainingAgents);

    public boolean isSpawnerFinished(double simTimeInSec) {
        if (isMaximumNumberOfSpawnedElementsReached()) {
            return true;
        }
        if (isSpawnerWithOneSingleSpawnEvent()) {
            return dynamicElementsCreatedTotal == spawnerAttributes.getEventElementCount();
        }
        return isAfterSourceEndTime(simTimeInSec) && isQueueEmpty();
    }

    public boolean isMaximumNumberOfSpawnedElementsReached() {
        final int maxNumber = spawnerAttributes.getConstraintsElementsMax();
        return maxNumber != AttributesSpawner.NO_MAX_SPAWN_NUMBER_TOTAL
                && dynamicElementsCreatedTotal >= maxNumber;
    }

    protected boolean isAfterSourceEndTime(double simTimeInSec) {
        return simTimeInSec > spawnerAttributes.getConstraintsTimeStart();
    }

    protected boolean isSpawnerWithOneSingleSpawnEvent() {
        return spawnerAttributes.getConstraintsTimeEnd() == spawnerAttributes.getConstraintsTimeStart();
    }

    abstract protected boolean isQueueEmpty();

    @Override
    public Attributes getAttributes() {
        return this.spawnerAttributes;
    }

    @Override
    public void setAttributes(Attributes attributes) {
        if(attributes instanceof AttributesSpawner)
            this.spawnerAttributes = (AttributesSpawner) attributes;
        else
            throw new IllegalArgumentException();
    }

}
