package org.vadere.state.scenario.spawner.impl;

import org.vadere.state.attributes.spawner.AttributesRegularSpawner;
import org.vadere.state.scenario.spawner.VSpawner;

import java.util.Random;

public class RegularSpawner extends VSpawner<AttributesRegularSpawner> {

    private int remainingSpawnAgents;
    private int spawnNumber;

    public RegularSpawner(AttributesRegularSpawner attributes, Random random) {
        super(attributes,random);
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

}
