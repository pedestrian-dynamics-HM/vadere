package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.health.InfectionStatus;

public class TransmissionModelSourceParameters extends Attributes {

    private int sourceId = -1;
    private boolean infectious = false;

    public TransmissionModelSourceParameters() {
    }

    public TransmissionModelSourceParameters(int sourceId, boolean infectious) {
        this.sourceId = sourceId;
        this.infectious = infectious;
    }


    public boolean isInfectious() {
        return infectious;
    }

    public int getSourceId() {
        return sourceId;
    }
}
