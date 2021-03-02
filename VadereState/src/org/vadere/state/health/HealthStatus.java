package org.vadere.state.health;

public class HealthStatus {

    // Member variables
    private InfectionStatus infectionStatus = InfectionStatus.SUSCEPTIBLE;
    private double lastInfectionStatusUpdateTime = -1;
    private double pathogenAbsorbedLoad = -1;

    // define infectious agent
    final private double pathogenEmissionCapacity; // potentially emitted pathogen load per update interval by infectious agent
    final private boolean isHighlyInfectious;
    // define susceptible agents
    final private double pathogenAbsorptionRate; // percentage of pathogen load that is absorbed by an agent that inhales aerosol cloud with certain pathogen load
    final private double susceptibility; // min absorbed pathogen load that leads to susceptible -> exposed (could be defined individually for each agent depending on its immune system)

    final private double exposedPeriod;     // defined individually; draw from distribution that is typical for disease
    final private double infectiousPeriod;  // defined individually; draw from distribution that is typical for disease
    final private double recoveredPeriod;   // defined individually; draw from distribution that is typical for disease

    // Constructors
    // ToDo: check if other constructors are necessary
    public HealthStatus() {

        // ToDo: define realistic default values
        pathogenEmissionCapacity = 5.0;
        isHighlyInfectious = false;
        pathogenAbsorptionRate = 0.1;
        susceptibility = 10;
        exposedPeriod = -1;
        infectiousPeriod = -1;
        recoveredPeriod = -1;
    }
    public HealthStatus(InfectionStatus infectionStatus, double lastInfectionStatusUpdateTime,
                        double pathogenAbsorbedLoad) {
        this.infectionStatus = infectionStatus;
        this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime;
        this.pathogenAbsorbedLoad = pathogenAbsorbedLoad;
        pathogenEmissionCapacity = 5.0;
        isHighlyInfectious = false;
        pathogenAbsorptionRate = 0.1;
        susceptibility = 10;
        exposedPeriod = -1;
        infectiousPeriod = -1;
        recoveredPeriod = -1;
    }


    // Getter
    public InfectionStatus getInfectionStatus() { return infectionStatus; }
    public double getLastInfectionStatusUpdateTime() { return lastInfectionStatusUpdateTime; }
    public double getPathogenAbsorbedLoad() { return pathogenAbsorbedLoad; }
    public double getPathogenEmissionCapacity() {return pathogenEmissionCapacity; }
    public boolean getIsHighlyInfectious() { return isHighlyInfectious; }
    public double getPathogenAbsorptionRate() { return pathogenAbsorptionRate ; }
    public double getSusceptibility() { return susceptibility; }
    public double getExposedPeriod() { return exposedPeriod; }
    public double getInfectiousPeriod() { return infectiousPeriod; }
    public double getRecoveredPeriod() { return recoveredPeriod; }

    // Setter
    public void setInfectionStatus(InfectionStatus infectionStatus) { this.infectionStatus = infectionStatus; }
    public void setLastInfectionStatusUpdateTime(double lastInfectionStatusUpdateTime) { this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime; }
    public void setPathogenAbsorbedLoad(double pathogenAbsorbedLoad) { this.pathogenAbsorbedLoad = pathogenAbsorbedLoad; }
}
