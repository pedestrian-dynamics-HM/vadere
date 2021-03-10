package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.health.InfectionStatus;

public class InfectionModelSourceParameters extends Attributes {

    private int sourceId = -1;
    private InfectionStatus infectionStatus = InfectionStatus.SUSCEPTIBLE;
    private double pedestrianPathogenEmissionCapacity = 1; // potentially emitted pathogen load per update interval by infectious agent
    private double pedestrianPathogenAbsorptionRate = 0.1; // percentage of pathogen load that is absorbed by an agent that inhales aerosol cloud with certain pathogen load
    private double pedestrianSusceptibility = 1; // min absorbed pathogen load that leads to susceptible -> exposed (could be defined individually for each agent depending on its immune system)
    private double exposedPeriod = 2*24*60*60;
    private double infectiousPeriod = 14*24*60*60;
    private double recoveredPeriod = 150*24*60*60;

    // ToDo define distributions

    public InfectionModelSourceParameters() {
    }

    public InfectionModelSourceParameters(int sourceId, InfectionStatus infectionStatus, double pedestrianPathogenEmissionCapacity, double pedestrianPathogenAbsorptionRate, double pedestrianSusceptibility, double exposedPeriod, double infectiousPeriod, double recoveredPeriod) {
        this.sourceId = sourceId;
        this.infectionStatus = infectionStatus;
        this.pedestrianPathogenEmissionCapacity = pedestrianPathogenEmissionCapacity;
        this.pedestrianPathogenAbsorptionRate = pedestrianPathogenAbsorptionRate;
        this.pedestrianSusceptibility = pedestrianSusceptibility;
        this.exposedPeriod = exposedPeriod;
        this.infectiousPeriod = infectiousPeriod;
        this.recoveredPeriod = recoveredPeriod;
    }


    public InfectionStatus getInfectionStatus() {
        return infectionStatus;
    }

    public double getPedestrianPathogenEmissionCapacity() {
        return pedestrianPathogenEmissionCapacity;
    }

    public double getPedestrianPathogenAbsorptionRate() {
        return pedestrianPathogenAbsorptionRate;
    }

    public double getPedestrianSusceptibility() {
        return pedestrianSusceptibility;
    }

    public double getExposedPeriod() {
        return exposedPeriod;
    }

    public double getInfectiousPeriod() {
        return infectiousPeriod;
    }

    public double getRecoveredPeriod() {
        return recoveredPeriod;
    }

    public int getSourceId() {
        return sourceId;
    }
}
