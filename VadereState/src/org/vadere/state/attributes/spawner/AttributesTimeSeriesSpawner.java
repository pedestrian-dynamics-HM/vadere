package org.vadere.state.attributes.spawner;

import java.util.List;

public class AttributesTimeSeriesSpawner extends AttributesSpawner {

    Double intervalLength;

    List<Integer> spawnsPerInterval;

    @Override
    public int getEventElementCount() {
        return 0;
    }

    @Override
    public void setEventElementCount(Integer eventElementCount) {

    }
}
