package org.vadere.state.medicine;

import org.vadere.state.scenario.Topography;

public class MedicineStatus {

    // Member variables
    private InfectionStatus infectionStatus;
    public double pathogenEmissionCapacity; // potential amount of emitted pathogens per update interval if infectious
    public double pathogenAbsorptionRate; // percentage of absorbed pathogens that are present at the current pedestrian's position per update interval
    public double absorbedAmountOfPathogen; // accumulated amount of pathogen
    public double susceptibility; // susceptibleToExposedThreshold (min exposure time or min amount of absorbed pathogen)
    public double exposedPeriod; // time between being exposed and infectious (not symptomatic)
    public double infectiousPeriod; // period of communicability
    public double recoveredPeriod;
    private double exposedStartTime;
    private double infectiousStartTime;
    private double recoveredStartTime;

    // Constructors
    // TODO: check which constructors are necessary? -> BK / SS
//    public MedicineStatus() {
//        this(???);
//    }

    public MedicineStatus(InfectionStatus infectionStatus, double pathogenEmissionCapacity,
                          double pathogenAbsorptionRate, double absorbedAmountOfPathogen, double susceptibility,
                          double exposedPeriod, double infectiousPeriod, double recoveredPeriod,
                          double exposedStartTime, double infectiousStartTime, double recoveredStartTime) {
        this.infectionStatus = infectionStatus;
        this.pathogenEmissionCapacity = pathogenEmissionCapacity;
        this.pathogenAbsorptionRate = pathogenAbsorptionRate;
        this.absorbedAmountOfPathogen = absorbedAmountOfPathogen;
        this.susceptibility = susceptibility;
        this.exposedPeriod = exposedPeriod;
        this.infectiousPeriod = infectiousPeriod;
        this.recoveredPeriod = recoveredPeriod;
        this.exposedStartTime = exposedStartTime;
        this.infectiousStartTime = infectiousStartTime;
        this.recoveredStartTime = recoveredStartTime;
    }

    public MedicineStatus(MedicineStatus other) {
        this.infectionStatus = other.getInfectionStatus();
        this.pathogenEmissionCapacity = other.pathogenEmissionCapacity;
        this.pathogenAbsorptionRate = other.pathogenAbsorptionRate;
        this.absorbedAmountOfPathogen = other.absorbedAmountOfPathogen;
        this.susceptibility = other.susceptibility;
        this.exposedPeriod = other.exposedPeriod;
        this.infectiousPeriod = other.infectiousPeriod;
        this.recoveredPeriod = other.recoveredPeriod;
        this.exposedStartTime = exposedStartTime;
        this.infectiousStartTime = infectiousStartTime;
        this.recoveredStartTime = recoveredStartTime;
    }

    // Getter
    public InfectionStatus getInfectionStatus() { return infectionStatus; }
    public double getPathogenEmissionCapacity() { return pathogenEmissionCapacity; }
    public double getPathogenAbsorptionRate() { return pathogenAbsorptionRate; }
    public double getAbsorbedAmountOfPathogen() {return absorbedAmountOfPathogen; }
    public double getSusceptibility() { return susceptibility; }
    public double getExposedPeriod() { return exposedPeriod; }
    public double getInfectiousPeriod() {return infectiousPeriod; }
    public double getRecoveredPeriod() {return recoveredPeriod; }
    public double getExposedStartTime() { return exposedStartTime; }
    public double getInfectiousStartTime() { return infectiousStartTime; }
    public double getRecoveredStartTime() { return recoveredStartTime; }



    // Setter
    public void setInfectionStatus(InfectionStatus infectionStatus) { this.infectionStatus = infectionStatus; }
    public void setPathogenEmissionCapacity(double pathogenEmissionCapacity) { this.pathogenEmissionCapacity = pathogenEmissionCapacity; }
    public void setPathogenAbsorptionRate(double pathogenAbsorptionRate) { this.pathogenAbsorptionRate = pathogenAbsorptionRate; }
    public void setAbsorbedAmountOfPathogen(double absorbedAmountOfPathogen) { this.absorbedAmountOfPathogen = absorbedAmountOfPathogen; }
    public void setSusceptibility(double susceptibility) { this.susceptibility = susceptibility; }
    public void setExposedPeriod(double exposedPeriod) { this.exposedPeriod = exposedPeriod; }
    public void setInfectiousPeriod(double infectiousPeriod) { this.infectiousPeriod = infectiousPeriod; }
    public void setRecoveredPeriod(double recoveredPeriod) { this.recoveredPeriod = recoveredPeriod; }
    public void setExposedStartTime(double exposedStartTime) { this.exposedStartTime = exposedStartTime; }
    public void setInfectiousStartTime(double infectiousStartTime) { this.infectiousStartTime = infectiousStartTime; }
    public void setRecoveredStartTime(double recoveredStartTime) { this.recoveredStartTime = recoveredStartTime; }
}
