package org.vadere.state.attributes.spawner;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesVisualElement;

public class AttributesRegularSpawner extends AttributesSpawner{
    protected Integer eventElementCount; //was spawnNumber

    @Override
    public int getEventElementCount() {
        return eventElementCount;
    }

    @Override
    public void setEventElementCount(Integer eventElementCount) {
        checkSealed();
        this.eventElementCount = eventElementCount;
    }

}
