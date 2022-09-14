package org.vadere.state.scenario.spawner;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.AttributesAttached;
import org.vadere.state.scenario.distribution.DistributionFactory;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.spawner.impl.LERPSpawner;
import org.vadere.state.scenario.spawner.impl.MixedSpawner;
import org.vadere.state.scenario.spawner.impl.RegularSpawner;
import org.vadere.state.scenario.spawner.impl.TimeSeriesSpawner;

import java.util.Random;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegularSpawner.class, name = "org.vadere.state.scenario.spawner.impl.RegularSpawner"),
        @JsonSubTypes.Type(value = LERPSpawner.class, name = "org.vadere.state.scenario.spawner.impl.LERPSpawner"),
        @JsonSubTypes.Type(value = TimeSeriesSpawner.class, name = "org.vadere.state.scenario.spawner.impl.TimeSeriesSpawner"),
        @JsonSubTypes.Type(value = MixedSpawner.class, name = "org.vadere.state.scenario.spawner.impl.MixedSpawner")
})
public abstract class VSpawner<T extends AttributesSpawner>  extends AttributesAttached<T> {
    protected int dynamicElementsCreatedTotal = 0;
    private final VDistribution distribution;

    public VSpawner(T attributes, Random random) {
        this.attributes= attributes;
        try {
            this.distribution = DistributionFactory.create(attributes.getDistributionAttributes(), new JDKRandomGenerator(random.nextInt()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract int getEventElementCount(double timeCurrentEvent);

    public abstract int getRemainingSpawnAgents();


    public abstract void setRemainingSpawnAgents(int remainingAgents);

    public boolean isSpawnerFinished(double simTimeInSec) {
        if (isMaximumNumberOfSpawnedElementsReached()) {
            return true;
        }
        if (isSpawnerWithOneSingleSpawnEvent()) {
            return dynamicElementsCreatedTotal == getEventElementCount(simTimeInSec);
        }
        return isAfterSpawnerEndTime(simTimeInSec) && isQueueEmpty();
    }

    public boolean isMaximumNumberOfSpawnedElementsReached() {
        final int maxNumber = attributes.getConstraintsElementsMax();
        return maxNumber != AttributesSpawner.NO_MAX_SPAWN_NUMBER_TOTAL
                && dynamicElementsCreatedTotal >= maxNumber;
    }

    protected boolean isAfterSpawnerEndTime(double simTimeInSec) {
        return simTimeInSec > attributes.getConstraintsTimeStart();
    }

    protected boolean isSpawnerWithOneSingleSpawnEvent() {
        return attributes.getConstraintsTimeEnd() == attributes.getConstraintsTimeStart();
    }

    abstract protected boolean isQueueEmpty();
    @Override
    public T getAttributes(){return attributes;}

    @Override
    public void setAttributes(T attributes){this.attributes = attributes;}

    public double getNextSpawnTime(double timeCurrentEvent){
       return distribution.getNextSample(timeCurrentEvent);
    }

}
