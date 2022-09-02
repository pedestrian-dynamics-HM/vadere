package org.vadere.state.scenario.spawner;

import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.Topography;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.distribution.VDistribution;

public abstract class VSpawner  implements AttributesAttached {

    private final Topography topography;
    protected AttributesSpawner spawnerAttributes;

    protected VDistribution distribution;
    protected int dynamicElementsCreatedTotal;


    protected VSpawner(AttributesSpawner attributes,Topography topography,VDistribution distribution) {
        this.spawnerAttributes = attributes;
        this.topography = topography;
        this.distribution = distribution;
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
            return dynamicElementsCreatedTotal == spawnerAttributes.getSpawnNumber();
        }
        return isAfterSourceEndTime(simTimeInSec) && isQueueEmpty();
    }

    public boolean isMaximumNumberOfSpawnedElementsReached() {
        final int maxNumber = spawnerAttributes.getMaxSpawnNumberTotal();
        return maxNumber != AttributesSource.NO_MAX_SPAWN_NUMBER_TOTAL
                && dynamicElementsCreatedTotal >= maxNumber;
    }

    protected boolean isAfterSourceEndTime(double simTimeInSec) {
        return simTimeInSec > spawnerAttributes.getEndTime();
    }

    protected boolean isSpawnerWithOneSingleSpawnEvent() {
        return spawnerAttributes.getStartTime() == spawnerAttributes.getEndTime();
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
