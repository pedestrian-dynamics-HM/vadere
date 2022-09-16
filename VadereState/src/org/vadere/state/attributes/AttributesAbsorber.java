package org.vadere.state.attributes;

import org.vadere.util.reflection.VadereAttribute;

public class AttributesAbsorber extends AttributesEnabled {
    @VadereAttribute
    protected Double deletionDistance = 0.0;

    public  AttributesAbsorber(){
        super();
    }

    public AttributesAbsorber(boolean enabled){
        super(enabled);
    }

    public double getDeletionDistance() {
        return deletionDistance;
    }

    public void setDeletionDistance(double deletionDistance) {
        checkSealed();
        this.deletionDistance = deletionDistance;
    }
}
