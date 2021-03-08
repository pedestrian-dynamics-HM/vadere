package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.health.InfectionStatus;

public class InfectionModelSourceParameters extends Attributes {

    final private InfectionStatus infectionStatus = InfectionStatus.SUSCEPTIBLE;
    final private double pedestrianPathogenEmissionCapacity = 5; // potentially emitted pathogen load per update interval by infectious agent
    final private double pedestrianPathogenAbsorptionRate = 0.1; // percentage of pathogen load that is absorbed by an agent that inhales aerosol cloud with certain pathogen load
    final private double pedestrianSusceptibility = 10; // min absorbed pathogen load that leads to susceptible -> exposed (could be defined individually for each agent depending on its immune system)
    final private double exposedPeriod = 2*24*60*60;
    final private double infectiousPeriod = 14*24*60*60;
    final private double recoveredPeriod = 150*24*60*60;
    final private int sourceId = -1;
    // ToDo define distributions

    public InfectionModelSourceParameters() {
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
