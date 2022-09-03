package org.vadere.state.scenario.spawner.impl;

import org.vadere.state.attributes.spawner.AttributesRegularSpawner;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.spawner.VSpawner;

public class RegularSpawner extends VSpawner {

    private int remainingSpawnAgents;
    private int spawnNumber;

    protected VDistribution distribution;

    public RegularSpawner(AttributesSpawner attributes, Topography topography, VDistribution distribution) {
        super(attributes,topography,distribution);
    }

    @Override
    public int getSpawnNumber(double timeCurrentEvent) {
        return ((AttributesRegularSpawner)this.spawnerAttributes).getEventElementCount();
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
