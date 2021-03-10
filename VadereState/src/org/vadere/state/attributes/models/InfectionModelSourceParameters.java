package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.health.InfectionStatus;

public class InfectionModelSourceParameters extends Attributes {

    private int sourceId = -1;
    private InfectionStatus infectionStatus = InfectionStatus.SUSCEPTIBLE;
    private double pedestrianPathogenEmissionCapacity = 1; // potentially emitted pathogen load per update interval by infectious agent

    public InfectionModelSourceParameters() {
    }

    public InfectionModelSourceParameters(int sourceId, InfectionStatus infectionStatus, double pedestrianPathogenEmissionCapacity, double pedestrianPathogenAbsorptionRate, double pedestrianSusceptibility, double exposedPeriod, double infectiousPeriod, double recoveredPeriod) {
        this.sourceId = sourceId;
        this.infectionStatus = infectionStatus;
        this.pedestrianPathogenEmissionCapacity = pedestrianPathogenEmissionCapacity;
    }


    public InfectionStatus getInfectionStatus() {
        return infectionStatus;
    }

    public double getPedestrianPathogenEmissionCapacity() {
        return pedestrianPathogenEmissionCapacity;
    }

    public int getSourceId() {
        return sourceId;
    }
}
