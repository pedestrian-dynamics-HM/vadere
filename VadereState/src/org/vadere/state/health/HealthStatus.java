package org.vadere.state.health;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.state.scenario.Pedestrian;

/**
 * This class contains all attributes and methods that a {@link Pedestrian} needs to emit and absorb pathogen. The
 * {@link HealthStatus} is initialized und updated by the InfectionModel.
 *
 * <ul>
 *     <li>{@link #pathogenEmissionCapacity}: potentially emitted pathogen load (pathogen particles per breath) in
 *     decimal log scale per update interval by infectious agent define susceptible agents</li>
 *     <li>{@link #pathogenAbsorptionRate}: tidal volume in m^3; one could account for protective measures such as
 *     masks by multiplying the tidal volume by a "mask efficiency factor [0, 1]"</li>
 *     <li>{@link #pathogenAbsorbedLoad}: current pathogen load that a pedestrian has accumulated</li>
 *     <li>{@link #minInfectiousDose}: min absorbed pathogen load that leads to change from infectionStatus susceptible
 *     to exposed.</li>
 *     <li>{@link #exposedPeriod}, {@link #infectiousPeriod}, {@link #recoveredPeriod}: time that must pass until the
 *     infectionStatus changes to the next status.</li>
 * </ul>
 *
 *
 */
public class HealthStatus {

    // Member variables
    private InfectionStatus infectionStatus;
    private double lastInfectionStatusUpdateTime;
    private double pathogenAbsorbedLoad;
    private VPoint startBreatheOutPosition;
    private double respiratoryTimeOffset;
    private boolean breathingIn;

    private double pathogenEmissionCapacity;
    private double pathogenAbsorptionRate;
    private double minInfectiousDose;
    private double exposedPeriod;
    private double infectiousPeriod;
    private double recoveredPeriod;

    private final static double defaultPathogenLoad = 0.0;
    private final static double defaultUpdateTime = -1;

    // Constructors
    public HealthStatus(InfectionStatus infectionStatus, double lastInfectionStatusUpdateTime, double pathogenAbsorbedLoad,
                        VPoint startBreatheOutPosition, double respiratoryTimeOffset, boolean breathingIn,
                        double pathogenEmissionCapacity, double pathogenAbsorptionRate, double minInfectiousDose,
                        double exposedPeriod, double infectiousPeriod, double recoveredPeriod) {
        this.infectionStatus = infectionStatus;
        this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime;
        this.pathogenAbsorbedLoad = pathogenAbsorbedLoad;
        this.startBreatheOutPosition = startBreatheOutPosition;
        this.respiratoryTimeOffset = respiratoryTimeOffset;
        this.breathingIn = breathingIn;
        this.pathogenEmissionCapacity = pathogenEmissionCapacity;
        this.pathogenAbsorptionRate = pathogenAbsorptionRate;
        this.minInfectiousDose = minInfectiousDose;
        this.exposedPeriod = exposedPeriod;
        this.infectiousPeriod = infectiousPeriod;
        this.recoveredPeriod = recoveredPeriod;
    }

    public HealthStatus() {
        this(InfectionStatus.SUSCEPTIBLE, defaultUpdateTime, defaultPathogenLoad, null,
                -1, false, -1, -1, -1,
                -1, -1, -1);
    }

    public HealthStatus(HealthStatus other) {
        this.infectionStatus = other.getInfectionStatus();
        this.lastInfectionStatusUpdateTime = other.getLastInfectionStatusUpdateTime();
        this.pathogenAbsorbedLoad = other.getPathogenAbsorbedLoad();
        this.startBreatheOutPosition = other.getStartBreatheOutPosition();
        this.respiratoryTimeOffset = other.getRespiratoryTimeOffset();
        this.pathogenEmissionCapacity = other.getPathogenEmissionCapacity();
        this.pathogenAbsorptionRate = other.getPathogenAbsorptionRate();
        this.minInfectiousDose = other.getMinInfectiousDose();
        this.exposedPeriod = other.getExposedPeriod();
        this.infectiousPeriod = other.getInfectiousPeriod();
        this.recoveredPeriod = other.getRecoveredPeriod();
        this.breathingIn = other.isBreathingIn();
    }


    // Getter
    public InfectionStatus getInfectionStatus() {
        return infectionStatus;
    }

    public double getLastInfectionStatusUpdateTime() {
        return lastInfectionStatusUpdateTime;
    }

    public double getPathogenAbsorbedLoad() {
        return pathogenAbsorbedLoad;
    }

    public VPoint getStartBreatheOutPosition() {
        return startBreatheOutPosition;
    }

    public double getRespiratoryTimeOffset() {
        return respiratoryTimeOffset;
    }

    public double getPathogenEmissionCapacity() {
        return pathogenEmissionCapacity;
    }

    public double getPathogenAbsorptionRate() {
        return pathogenAbsorptionRate;
    }

    public double getMinInfectiousDose() {
        return minInfectiousDose;
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

    public boolean isBreathingIn() {
        return breathingIn;
    }

    // Setter
    public void setInfectionStatus(InfectionStatus infectionStatus) {
        this.infectionStatus = infectionStatus;
    }

    public void setLastInfectionStatusUpdateTime(double lastInfectionStatusUpdateTime) {
        this.lastInfectionStatusUpdateTime = lastInfectionStatusUpdateTime;
    }

    public void setPathogenAbsorbedLoad(double pathogenAbsorbedLoad) {
        this.pathogenAbsorbedLoad = pathogenAbsorbedLoad;
    }

    public void setStartBreatheOutPosition(VPoint startBreatheOutPosition) {
        this.startBreatheOutPosition = startBreatheOutPosition;
    }

    public void setRespiratoryTimeOffset(double respiratoryTimeOffset) {
        this.respiratoryTimeOffset = respiratoryTimeOffset;
    }

    public void setPathogenEmissionCapacity(double pathogenEmissionCapacity) {
        this.pathogenEmissionCapacity = pathogenEmissionCapacity;
    }

    public void setPathogenAbsorptionRate(double pathogenAbsorptionRate) {
        this.pathogenAbsorptionRate = pathogenAbsorptionRate;
    }

    public void setMinInfectiousDose(double minInfectiousDose) {
        this.minInfectiousDose = minInfectiousDose;
    }

    public void setExposedPeriod(double exposedPeriod) {
        this.exposedPeriod = exposedPeriod;
    }

    public void setInfectiousPeriod(double infectiousPeriod) {
        this.infectiousPeriod = infectiousPeriod;
    }

    public void setRecoveredPeriod(double recoveredPeriod) {
        this.recoveredPeriod = recoveredPeriod;
    }

    public void setBreathingIn(boolean breathingIn) {
        this.breathingIn = breathingIn;
    }

    // other methods
    public double emitPathogen() {
        return Math.pow(10, this.getPathogenEmissionCapacity());
    }

    public void updateRespiratoryCycle(double simTimeInSec, double periodLength) {
        // Assumption: phases when breathing in and out are equally long
        // Breathing in phase condition: sin(time) > 0 or cos(time) == 1
        double b = 2.0 * Math.PI / periodLength;
        setBreathingIn((Math.sin(b * (respiratoryTimeOffset + simTimeInSec)) > 0) || (Math.cos(b * (respiratoryTimeOffset + simTimeInSec)) == 1));
    }

    public boolean isStartingBreatheOut() {
        return (!breathingIn && startBreatheOutPosition == null);
    }

    public boolean isStartingBreatheIn() {
        return (breathingIn && !(startBreatheOutPosition == null));
    }

    public void absorbPathogen(double pathogenConcentration) {
        double accumulatedAbsorbedPathogenLoad = pathogenAbsorbedLoad + pathogenAbsorptionRate * pathogenConcentration;

        switch (infectionStatus) {
            case SUSCEPTIBLE:
            case EXPOSED:
                setPathogenAbsorbedLoad(accumulatedAbsorbedPathogenLoad);
                break;
            case INFECTIOUS:
            case RECOVERED:
                break; // do not absorb
            default:
                throw new IllegalStateException("Unexpected value: " + infectionStatus);
        }
    }

    public void updateInfectionStatus(double simTimeInSec) {
        switch (infectionStatus) {
            case SUSCEPTIBLE:
                if (pathogenAbsorbedLoad >= minInfectiousDose) {
                    setInfectionStatus(InfectionStatus.EXPOSED);
                    setLastInfectionStatusUpdateTime(simTimeInSec);
                }
                break;
            case EXPOSED:
                if (simTimeInSec >= lastInfectionStatusUpdateTime + exposedPeriod) {
                    setInfectionStatus(InfectionStatus.INFECTIOUS);
                    setLastInfectionStatusUpdateTime(simTimeInSec);
                }
                break;
            case INFECTIOUS:
                if (simTimeInSec >= lastInfectionStatusUpdateTime + infectiousPeriod) {
                    setInfectionStatus(InfectionStatus.RECOVERED);
                    setLastInfectionStatusUpdateTime(simTimeInSec);
                    setPathogenAbsorbedLoad(defaultPathogenLoad); // reset pathogen load to 0
                }
                break;
            case RECOVERED:
                if (simTimeInSec >= lastInfectionStatusUpdateTime + recoveredPeriod) {
                    setInfectionStatus(InfectionStatus.SUSCEPTIBLE);
                    setLastInfectionStatusUpdateTime(defaultUpdateTime); // reset lastInfectionStatusUpdateTime
                }
                break;
        }
    }
}
