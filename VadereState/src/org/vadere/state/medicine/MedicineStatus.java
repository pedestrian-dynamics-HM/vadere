package org.vadere.state.medicine;

public class MedicineStatus {

    // Member variables
    private InfectionStatus infectionStatus;
    public double pathogenEmissionCapacity; // potential amount of emitted pathogens per update interval if infectious
    public double pathogenAbsorptionRate; // percentage of absorbed pathogens that are present at the current pedestrian's position per update interval
    public double absorbedAmountOfPathogen; // accumulated amount of pathogen
    public double susceptibility; // susceptibleToExposedThreshold (min exposure time or min amount of absorbed pathogen)
    public double latentPeriod; // "exposedPeriod" or time between being exposed and infectious (not symptomatic)
    public double infectiousPeriod; // period of communicability
    public double recoveredPeriod;

    // Constructors
    // TODO: check which constructors are necessary? -> BK / SS
//    public MedicineStatus() {
//        this(???);
//    }

    public MedicineStatus(InfectionStatus infectionStatus, double pathogenEmissionCapacity,
                          double pathogenAbsorptionRate, double absorbedAmountOfPathogen, double susceptibility,
                          double latentPeriod, double infectiousPeriod, double recoveredPeriod) {
        this.infectionStatus = infectionStatus;
        this.pathogenEmissionCapacity = pathogenEmissionCapacity;
        this.pathogenAbsorptionRate = pathogenAbsorptionRate;
        this.absorbedAmountOfPathogen = absorbedAmountOfPathogen;
        this.susceptibility = susceptibility;
        this.latentPeriod = latentPeriod;
        this.infectiousPeriod = infectiousPeriod;
        this.recoveredPeriod = recoveredPeriod;
    }

    public MedicineStatus(MedicineStatus other) {
        this.infectionStatus = other.getInfectionStatus();
        this.pathogenEmissionCapacity = other.pathogenEmissionCapacity;
        this.pathogenAbsorptionRate = other.pathogenAbsorptionRate;
        this.absorbedAmountOfPathogen = other.absorbedAmountOfPathogen;
        this.susceptibility = other.susceptibility;
        this.latentPeriod = other.latentPeriod;
        this.infectiousPeriod = other.infectiousPeriod;
        this.recoveredPeriod = other.recoveredPeriod;
    }

    // Getter
    public InfectionStatus getInfectionStatus() { return infectionStatus; }
    public double getPathogenEmissionCapacity() { return pathogenEmissionCapacity; }
    public double getPathogenAbsorptionRate() { return pathogenAbsorptionRate; }
    public double getAbsorbedAmountOfPathogen() {return absorbedAmountOfPathogen; }
    public double getSusceptibility() { return susceptibility; }
    public double getLatentPeriod() { return latentPeriod; }
    public double getInfectiousPeriod() {return infectiousPeriod; }
    public double getRecoveredPeriod() {return recoveredPeriod; }

    // Setter
    public void setInfectionStatus(InfectionStatus infectionStatus) { this.infectionStatus = infectionStatus; }
    public void setPathogenEmissionCapacity(double pathogenEmissionCapacity) { this.pathogenEmissionCapacity = pathogenEmissionCapacity; }
    public void setPathogenAbsorptionRate(double pathogenAbsorptionRate) { this.pathogenAbsorptionRate = pathogenAbsorptionRate; }
    public void setAbsorbedAmountOfPathogen(double absorbedAmountOfPathogen) { this.absorbedAmountOfPathogen = absorbedAmountOfPathogen; }
    public void setSusceptibility(double susceptibility) { this.susceptibility = susceptibility; }
    public void setLatentPeriod(double latentPeriod) { this.latentPeriod = latentPeriod; }
    public void setInfectiousPeriod(double infectiousPeriod) { this.infectiousPeriod = infectiousPeriod; }
    public void setRecoveredPeriod(double recoveredPeriod) { this.recoveredPeriod = recoveredPeriod; }

}
