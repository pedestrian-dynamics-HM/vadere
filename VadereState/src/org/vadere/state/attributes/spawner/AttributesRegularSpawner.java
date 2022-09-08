package org.vadere.state.attributes.spawner;

import org.vadere.util.reflection.VadereAttribute;

public class AttributesRegularSpawner extends AttributesSpawner{
    @VadereAttribute
    protected Integer eventElementCount = 0;

    public AttributesRegularSpawner(){
        super();
    }

    public int getEventElementCount() {
        return eventElementCount;
    }

    public void setEventElementCount(Integer eventElementCount) {
        checkSealed();
        this.eventElementCount = eventElementCount;
    }
}
