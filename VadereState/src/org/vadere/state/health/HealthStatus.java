package org.vadere.state.health;

import org.vadere.util.geometry.shapes.VPoint;

public class HealthStatus {

    // Member variables
    private InfectionStatus infectionStatus;
    private double lastInfectionStatusUpdateTime = -1;
    private double pathogenAbsorbedLoad = 0.0;
    private VPoint startBreatheOutPosition = null;

    // define infectious agent
    private double pathogenEmissionCapacity; // potentially emitted pathogen load (pathogen particles per breath) in decimal log scale per update interval by infectious agent
    // define susceptible agents
    private double pathogenAbsorptionRate = 0.005; // tidal volume in m^3; one could account for protective measures such as masks by multiplying the tidal volume by a "mask efficiency factor [0, 1]"
    private double susceptibility; // min absorbed pathogen load that leads to susceptible -> exposed (could be defined individually for each agent depending on its immune system)

    private double exposedPeriod;
    private double infectiousPeriod;
    private double recoveredPeriod;

    // Constructors
    public HealthStatus() {
    }

    public HealthStatus(InfectionStatus infectionStatus, double lastInfectionStatusUpdateTime, double pathogenAbsorbedLoad,
                        VPoint startBreatheOutPosition,
                        double pathogenEmissionCapacity, double pathogenAbsorptionRate,
                        double susceptibility, double exposedPeriod, double infectiousPeriod, double recoveredPeriod) {
        this.infectionStatus = infectionStatus;
        this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime;
        this.pathogenAbsorbedLoad = pathogenAbsorbedLoad;
        this.startBreatheOutPosition = startBreatheOutPosition;
        this.pathogenEmissionCapacity = pathogenEmissionCapacity;
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
        this.startBreatheOutPosition = other.getStartBreatheOutPosition();
        this.pathogenEmissionCapacity = other.getPathogenEmissionCapacity();
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
    public VPoint getStartBreatheOutPosition() { return startBreatheOutPosition; }
    public double getPathogenEmissionCapacity() {return pathogenEmissionCapacity; }
    public double getPathogenAbsorptionRate() { return pathogenAbsorptionRate ; }
    public double getSusceptibility() { return susceptibility; }
    public double getExposedPeriod() { return exposedPeriod; }
    public double getInfectiousPeriod() { return infectiousPeriod; }
    public double getRecoveredPeriod() { return recoveredPeriod; }

    // Setter
    public void setInfectionStatus(InfectionStatus infectionStatus) { this.infectionStatus = infectionStatus; }
    public void setLastInfectionStatusUpdateTime(double lastInfectionStatusUpdateTime) { this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime; }
    public void setPathogenAbsorbedLoad(double pathogenAbsorbedLoad) { this.pathogenAbsorbedLoad = pathogenAbsorbedLoad; }
    public void setStartBreatheOutPosition(VPoint startBreatheOutPosition) { this.startBreatheOutPosition = startBreatheOutPosition; }
    public void setPathogenEmissionCapacity(double pathogenEmissionCapacity) { this.pathogenEmissionCapacity = pathogenEmissionCapacity; }
    public void setPathogenAbsorptionRate(double pathogenAbsorptionRate) { this.pathogenAbsorptionRate = pathogenAbsorptionRate; }
    public void setSusceptibility(double susceptibility) { this.susceptibility = susceptibility; }
    public void setExposedPeriod(double exposedPeriod) { this.exposedPeriod = exposedPeriod; }
    public void setInfectiousPeriod(double infectiousPeriod) { this.infectiousPeriod = infectiousPeriod; }
    public void setRecoveredPeriod(double recoveredPeriod) { this.recoveredPeriod = recoveredPeriod; }

    // other methods
    public double emitPathogen() { return Math.pow(10, this.getPathogenEmissionCapacity()); }
}
