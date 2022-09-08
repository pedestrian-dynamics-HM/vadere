package org.vadere.state.scenario.spawner.impl;

import org.vadere.state.attributes.spawner.AttributesRegularSpawner;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.spawner.VSpawner;

public class RegularSpawner extends VSpawner<AttributesRegularSpawner> {

    private int remainingSpawnAgents;
    private int spawnNumber;

    protected VDistribution distribution;

    public RegularSpawner(){
        super(new AttributesRegularSpawner());
    }
    public RegularSpawner(AttributesRegularSpawner attributes) {
        super(attributes);
    }

    @Override
    public int getEventElementCount(double timeCurrentEvent) {
        return this.attributes.getEventElementCount();
    }

    @Override
    public int getRemainingSpawnAgents() {
        return this.remainingSpawnAgents;
    }

    @Override
    public void setRemainingSpawnAgents(int remainingAgents) {
        this.remainingSpawnAgents = remainingAgents;
    }

    @Override
    protected boolean isQueueEmpty() {
        return false;
    }

    @Override
    public void update(double simTimeInSec) {
    }
}
