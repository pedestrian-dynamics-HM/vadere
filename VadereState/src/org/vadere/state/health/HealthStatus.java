package org.vadere.state.health;


public class HealthStatus {

    // Member variables
    private InfectionStatus infectionStatus = InfectionStatus.SUSCEPTIBLE;
    private double lastInfectionStatusUpdateTime = -1;
    private double pathogenAbsorbedLoad = -1;

    // define infectious agent
    // ToDo: define realistic default values
    final private double pathogenEmissionCapacity = 5.0; // potentially emitted pathogen load per update interval by infectious agent
    final private boolean isHighlyInfectious = false;
    // define susceptible agents
    final private double pathogenAbsorptionRate = 0.1; // percentage of pathogen load that is absorbed by an agent that inhales aerosol cloud with certain pathogen load
    final private double susceptibility = 10; // min absorbed pathogen load that leads to susceptible -> exposed (could be defined individually for each agent depending on its immune system)
    // defined individually; draw from distribution that is typical for disease
    final private double exposedPeriod = -1;
    final private double infectiousPeriod = -1;
    final private double recoveredPeriod = -1;

    // Constructors
    // ToDo: check if other constructors are necessary
    public HealthStatus(InfectionStatus infectionStatus, double lastInfectionStatusUpdateTime,
                        double pathogenAbsorbedLoad) {
        this.infectionStatus = infectionStatus;
        this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime;
        this.pathogenAbsorbedLoad = pathogenAbsorbedLoad;
    }

    // Getter
    public InfectionStatus getInfectionStatus() { return infectionStatus; }
    public double lastInfectionStatusUpdateTime() { return lastInfectionStatusUpdateTime; }
    public double pathogenAbsorbedLoad() { return pathogenAbsorbedLoad; }
    public double pathogenEmissionCapacity() {return pathogenEmissionCapacity; }
    public boolean isHighlyInfectious() { return isHighlyInfectious; }
    public double pathogenAbsorptionRate() { return pathogenAbsorptionRate ; }
    public double susceptibility() { return susceptibility; }
    public double exposedPeriod() { return exposedPeriod; }
    public double infectiousPeriod() { return infectiousPeriod; }
    public double recoveredPeriod() { return recoveredPeriod; }

    // Setter
    public void setInfectionStatus(InfectionStatus infectionStatus) { this.infectionStatus = infectionStatus; }
    public void setLastInfectionStatusUpdateTime(double lastInfectionStatusUpdateTime) { this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime; }
    public void setPathogenAbsorbedLoad(double pathogenAbsorbedLoad) { this.pathogenAbsorbedLoad = pathogenAbsorbedLoad; }
}
