package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.health.InfectionStatus;

public class InfectionModelSourceParameters extends Attributes {

    private int sourceId = -1;
    private InfectionStatus infectionStatus = InfectionStatus.SUSCEPTIBLE;

    public InfectionModelSourceParameters() {
    }

    public InfectionModelSourceParameters(int sourceId, InfectionStatus infectionStatus) {
        this.sourceId = sourceId;
        this.infectionStatus = infectionStatus;
    }


    public InfectionStatus getInfectionStatus() {
        return infectionStatus;
    }

    public int getSourceId() {
        return sourceId;
    }
}
