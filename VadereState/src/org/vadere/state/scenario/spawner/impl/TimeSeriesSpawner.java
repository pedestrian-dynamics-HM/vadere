package org.vadere.state.scenario.spawner.impl;

import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.distribution.impl.TimeSeriesDistribution;
import org.vadere.state.scenario.spawner.VSpawner;

import java.util.ArrayList;

public class TimeSeriesSpawner extends RegularSpawner {

    Double[] switchpoints;
    private int currentInterval = 0;
    ArrayList<VSpawner> spawners;

    protected TimeSeriesSpawner(AttributesSpawner attributes, Topography topography,TimeSeriesDistribution distribution) {
        super(attributes, topography,distribution);
    }

    @Override
    public int getSpawnNumber(double timeCurrentEvent) {
        return getSpawnerByTime(timeCurrentEvent).getSpawnNumber(timeCurrentEvent);
    }

    @Override
    public int getRemainingSpawnAgents() {
        return spawners.get(currentInterval).getRemainingSpawnAgents();
    }

    @Override
    public void update(double simTimeInSec) {

    }

    @Override
    public void setRemainingSpawnAgents(int remainingAgents) {
        spawners.get(currentInterval).setRemainingSpawnAgents(remainingAgents);
    }

    @Override
    protected boolean isQueueEmpty() {
        return false;
    }

    private VSpawner getSpawnerByTime(double timeCurrentEvent) {
        while (!(currentInterval > switchpoints.length - 1) && timeCurrentEvent >= switchpoints[currentInterval]
                && !(timeCurrentEvent > switchpoints[switchpoints.length - 1])) {
            currentInterval++;
        }

        return spawners.get(currentInterval);
    }

}
