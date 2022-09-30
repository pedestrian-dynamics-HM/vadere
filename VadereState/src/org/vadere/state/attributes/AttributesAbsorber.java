package org.vadere.state.attributes;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.util.Views;
import org.vadere.util.reflection.VadereAttribute;
/**
 * This component controls the absorbing behaviour of this absorbing area.
 */
public class AttributesAbsorber extends AttributesEnabled {
    /**
     * This attribute stores the distance an agent need to be away from the absorbing area to be absorbed.
     */
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected Double deletionDistance = 0.0;

    public  AttributesAbsorber(){
        super();
    }

    public AttributesAbsorber(boolean enabled){
        super(enabled);
    }

    public AttributesAbsorber(boolean enabled, double deletionDistance){
        super(enabled);
        this.deletionDistance = deletionDistance;
    }

    public double getDeletionDistance() {
        return deletionDistance;
    }

    public void setDeletionDistance(double deletionDistance) {
        checkSealed();
        this.deletionDistance = deletionDistance;
    }
}
