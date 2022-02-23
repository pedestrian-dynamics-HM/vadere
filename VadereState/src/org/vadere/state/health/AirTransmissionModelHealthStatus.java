package org.vadere.state.health;

import org.vadere.util.geometry.shapes.VPoint;

public class AirTransmissionModelHealthStatus extends ExposureModelHealthStatus {

    private boolean breathingIn;

    /*
     * Defines the start of each pedestrian's respiratory cycle. This allows to have individual respiratory cycles for
     * all pedestrians, i.e. they in- or exhale all at different times.
     */
    private double respiratoryTimeOffset;

    /*
     * defines the position at which pedestrian starts current exhalation;
     */
    private VPoint exhalationStartPosition;

    /*
     * reset value for simulation periods during which pedestrian inhales
     */
    private final static VPoint RESET_EXHALATION_POSITION = null;

    // Constructors
    public AirTransmissionModelHealthStatus() {
        this(false, 0, RESET_EXHALATION_POSITION);
    }

    public AirTransmissionModelHealthStatus(boolean breathingIn, double respiratoryTimeOffset, VPoint exhalationStartPosition) {
        super();
        this.breathingIn = breathingIn;
        this.respiratoryTimeOffset = respiratoryTimeOffset;
        this.exhalationStartPosition = exhalationStartPosition;
    }

    public AirTransmissionModelHealthStatus(AirTransmissionModelHealthStatus other) {
        super(other.isInfectious(), other.getDegreeOfExposure());
        this.breathingIn = other.isBreathingIn();
        this.respiratoryTimeOffset = other.getRespiratoryTimeOffset();
        this.exhalationStartPosition = other.getExhalationStartPosition();
    }


    // Getter
    @Override
    public boolean isInfectious() {
        return infectious;
    }

    @Override
    public double getDegreeOfExposure() {
        return degreeOfExposure;
    }

    public boolean isBreathingIn() {
        return breathingIn;
    }

    public double getRespiratoryTimeOffset() {
        return respiratoryTimeOffset;
    }

    public VPoint getExhalationStartPosition() {
        return exhalationStartPosition;
    }

    // Setter
    @Override
    public void setInfectious(boolean infectious) {
        this.infectious = infectious;
    }

    @Override
    public void setDegreeOfExposure(double degreeOfExposure) {
        this.degreeOfExposure = degreeOfExposure;
    }

    public void setBreathingIn(boolean breathingIn) {
        this.breathingIn = breathingIn;
    }

    public void setRespiratoryTimeOffset(double respiratoryTimeOffset) {
        this.respiratoryTimeOffset = respiratoryTimeOffset;
    }

    public void setExhalationStartPosition(VPoint exhalationStartPosition) {
        this.exhalationStartPosition = exhalationStartPosition;
    }

    // Methods

    public void incrementDegreeOfExposure(double deltaDegreeOfExposure) {
        this.degreeOfExposure += deltaDegreeOfExposure;
    }

    /*
     * Defines whether the pedestrian inhales or exhales depending on the current simulation time,
     * respiratoryTimeOffset, and periodLength. Assumes that periodLength for inhalation and exhalation are equally
     * long. Pedestrian inhales when sin(time) > 0 or cos(time) == 1.
     */
    public void updateRespiratoryCycle(double simTimeInSec, double periodLength) {
        double b = 2.0 * Math.PI / periodLength;
        setBreathingIn((Math.sin(b * (respiratoryTimeOffset + simTimeInSec)) > 0) || (Math.cos(b * (respiratoryTimeOffset + simTimeInSec)) == 1));
    }

    public boolean isStartingExhalation() {
        return (!breathingIn && exhalationStartPosition == RESET_EXHALATION_POSITION);
    }

    public boolean isStartingInhalation() {
        return (breathingIn && !(exhalationStartPosition == RESET_EXHALATION_POSITION));
    }

    public void resetStartExhalationPosition() {
        exhalationStartPosition = RESET_EXHALATION_POSITION;
    }
}
