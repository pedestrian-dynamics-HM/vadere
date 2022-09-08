package org.vadere.state.attributes.spawner;

import java.util.List;

public class AttributesTimeSeriesSpawner extends AttributesSpawner {

    Double intervalLength;

    List<Integer> spawnsPerInterval;

    public Double getIntervalLength() {
        return intervalLength;
    }

    public void setIntervalLength(Double intervalLength) {
        this.intervalLength = intervalLength;
    }

    public List<Integer> getSpawnsPerInterval() {
        return spawnsPerInterval;
    }

    public void setSpawnsPerInterval(List<Integer> spawnsPerInterval) {
        this.spawnsPerInterval = spawnsPerInterval;
    }
}
