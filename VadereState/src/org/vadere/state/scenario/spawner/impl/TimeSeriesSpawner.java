package org.vadere.state.scenario.spawner.impl;

import org.vadere.state.attributes.spawner.AttributesTimeSeriesSpawner;
import org.vadere.state.scenario.spawner.VSpawner;

import java.util.ArrayList;

public class TimeSeriesSpawner extends VSpawner<AttributesTimeSeriesSpawner> {

    Double[] switchpoints;
    private final int currentInterval = 0;
    ArrayList<VSpawner> spawners;

    private MixedSpawner spawner;

    protected TimeSeriesSpawner(AttributesTimeSeriesSpawner attributes) {
        super(attributes);
    }

    @Override
    public int getSpawnNumber(double timeCurrentEvent) {
        return spawner.getSpawnNumber(timeCurrentEvent);
    }

    @Override
    public int getRemainingSpawnAgents() {
        return spawner.getRemainingSpawnAgents();
    }

    @Override
    public void setRemainingSpawnAgents(int remainingAgents) {
        spawner.setRemainingSpawnAgents(remainingAgents);
    }

    @Override
    public void update(double simTimeInSec) {

    }

    @Override
    protected boolean isQueueEmpty() {
        return false;
    }

}
