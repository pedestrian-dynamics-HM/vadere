package org.vadere.state.attributes.spawner;

import org.vadere.util.Attributes;

public class AttributesSpawner extends Attributes {
    protected Integer maxSpawnNumberTotal;
    protected Double endTime;
    protected Double startTime;


    public void setMaxSpawnNumberTotal(int maxSpawnNumberTotal) {
        checkSealed();
        this.maxSpawnNumberTotal = maxSpawnNumberTotal;
    }
    public int getMaxSpawnNumberTotal() {
        return maxSpawnNumberTotal;
    }

    public void setEndTime(double endTime) {
        checkSealed();
        this.endTime = endTime;
    }
    public double getEndTime() {
        return endTime;
    }

    public void setStartTime(double startTime) {
        checkSealed();
        this.startTime = startTime;
    }
    public double getStartTime() {
        return startTime;
    }
    public int getSpawnNumber() {
        throw new UnsupportedOperationException();
    }
    public void setSpawnNumber(int spawnNumber) {
        throw new UnsupportedOperationException();
    }

}
