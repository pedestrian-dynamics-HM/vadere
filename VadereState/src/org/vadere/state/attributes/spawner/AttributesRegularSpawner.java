package org.vadere.state.attributes.spawner;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesVisualElement;
import org.vadere.util.reflection.VadereAttribute;

public class AttributesRegularSpawner extends AttributesSpawner{
    @VadereAttribute
    protected Integer eventElementCount;

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
