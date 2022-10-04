package org.vadere.state.scenario.spawner.impl;

import org.vadere.state.attributes.spawner.AttributesTimeSeriesSpawner;
import org.vadere.state.scenario.spawner.VSpawner;

import java.util.Random;

public class TimeSeriesSpawner extends VSpawner<AttributesTimeSeriesSpawner> {

    private MixedSpawner spawner;

    public TimeSeriesSpawner(AttributesTimeSeriesSpawner attributes, Random random) {
        super(attributes,random);

    }

    @Override
    public int getEventElementCount(double timeCurrentEvent) {
        return spawner.getEventElementCount(timeCurrentEvent);
    }

    @Override
    public int getRemainingSpawnAgents() {
        return spawner.getRemainingSpawnAgents();
    }

    @Override
    public void setRemainingSpawnAgents(int remainingAgents) {
        spawner.setRemainingSpawnAgents(remainingAgents);
    }

}
