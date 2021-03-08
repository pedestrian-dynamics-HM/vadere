package org.vadere.state.health;

public class HealthStatus {

    // Member variables
    private InfectionStatus infectionStatus;
    private double lastInfectionStatusUpdateTime = -1;
    private double pathogenAbsorbedLoad = 0.0;

    // define infectious agent
    private double pathogenEmissionCapacity; // potentially emitted pathogen load per update interval by infectious agent
    private boolean HighlyInfectious;
    // define susceptible agents
    private double pathogenAbsorptionRate; // percentage of pathogen load that is absorbed by an agent that inhales aerosol cloud with certain pathogen load
    private double susceptibility; // min absorbed pathogen load that leads to susceptible -> exposed (could be defined individually for each agent depending on its immune system)

    private double exposedPeriod;
    private double infectiousPeriod;
    private double recoveredPeriod;

    // Constructors
    public HealthStatus() {
//        // ToDo: define realistic default values
//        this.infectionStatus = InfectionStatus.SUSCEPTIBLE;
//        this.lastInfectionStatusUpdateTime = -1;
//        this.pathogenAbsorbedLoad = 0.0;
//        this.pathogenEmissionCapacity = 5.0;
//        this.HighlyInfectious = false;
//        this.pathogenAbsorptionRate = 0.1;
//        this.susceptibility = 10;
//        this.exposedPeriod = 2*24*60*60;    // ToDo should be drawn from distribution
//        this.infectiousPeriod = 14*24*60*60;    // ToDo should be drawn from distribution
//        this.recoveredPeriod = 150*24*60*60;    // ToDo should be drawn from distribution
    }

    public HealthStatus(InfectionStatus infectionStatus, double lastInfectionStatusUpdateTime, double pathogenAbsorbedLoad,
                        double pathogenEmissionCapacity, boolean highlyInfectious, double pathogenAbsorptionRate,
                        double susceptibility, double exposedPeriod, double infectiousPeriod, double recoveredPeriod) {
        this.infectionStatus = infectionStatus;
        this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime;
        this.pathogenAbsorbedLoad = pathogenAbsorbedLoad;
        this.pathogenEmissionCapacity = pathogenEmissionCapacity;
        this.HighlyInfectious = highlyInfectious;
        this.pathogenAbsorptionRate = pathogenAbsorptionRate;
        this.susceptibility = susceptibility;
        this.exposedPeriod = exposedPeriod;
        this.infectiousPeriod = infectiousPeriod;
        this.recoveredPeriod = recoveredPeriod;
    }

    public HealthStatus(HealthStatus other) {
        this.infectionStatus = other.getInfectionStatus();
        this.lastInfectionStatusUpdateTime = other.getLastInfectionStatusUpdateTime();
        this.pathogenAbsorbedLoad = other.getPathogenAbsorbedLoad();
        this.pathogenEmissionCapacity = other.getPathogenEmissionCapacity();
        this.HighlyInfectious = other.isHighlyInfectious();
        this.pathogenAbsorptionRate = other.getPathogenAbsorptionRate();
        this.susceptibility = other.getSusceptibility();
        this.exposedPeriod = other.getExposedPeriod();
        this.infectiousPeriod = other.getInfectiousPeriod();
        this.recoveredPeriod = other.getRecoveredPeriod();
    }


    // Getter
    public InfectionStatus getInfectionStatus() { return infectionStatus; }
    public double getLastInfectionStatusUpdateTime() { return lastInfectionStatusUpdateTime; }
    public double getPathogenAbsorbedLoad() { return pathogenAbsorbedLoad; }
    public double getPathogenEmissionCapacity() {return pathogenEmissionCapacity; }
    public boolean isHighlyInfectious() { return HighlyInfectious; }
    public double getPathogenAbsorptionRate() { return pathogenAbsorptionRate ; }
    public double getSusceptibility() { return susceptibility; }
    public double getExposedPeriod() { return exposedPeriod; }
    public double getInfectiousPeriod() { return infectiousPeriod; }
    public double getRecoveredPeriod() { return recoveredPeriod; }

    // Setter
    public void setInfectionStatus(InfectionStatus infectionStatus) { this.infectionStatus = infectionStatus; }
    public void setLastInfectionStatusUpdateTime(double lastInfectionStatusUpdateTime) { this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime; }
    public void setPathogenAbsorbedLoad(double pathogenAbsorbedLoad) { this.pathogenAbsorbedLoad = pathogenAbsorbedLoad; }
    public void setPathogenEmissionCapacity(double pathogenEmissionCapacity) { this.pathogenEmissionCapacity = pathogenEmissionCapacity; }
    public void setHighlyInfectious(boolean highlyInfectious) { HighlyInfectious = highlyInfectious; }
    public void setPathogenAbsorptionRate(double pathogenAbsorptionRate) { this.pathogenAbsorptionRate = pathogenAbsorptionRate; }
    public void setSusceptibility(double susceptibility) { this.susceptibility = susceptibility; }
    public void setExposedPeriod(double exposedPeriod) { this.exposedPeriod = exposedPeriod; }
    public void setInfectiousPeriod(double infectiousPeriod) { this.infectiousPeriod = infectiousPeriod; }
    public void setRecoveredPeriod(double recoveredPeriod) { this.recoveredPeriod = recoveredPeriod; }
}
