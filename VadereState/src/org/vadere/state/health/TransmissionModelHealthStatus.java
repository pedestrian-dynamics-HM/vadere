package org.vadere.state.health;

import org.vadere.util.geometry.shapes.VPoint;

public class TransmissionModelHealthStatus implements ExposureModelHealthStatus {

    // Member variables
    boolean isInfectious;
    double degreeOfExposure;

    boolean isBreathingIn;
    double respiratoryTimeOffset;

    /*
     * defines the position at which pedestrian starts current exhalation;
     */
    VPoint exhalationStartPosition;

    /*
     * reset value for simulation periods during which pedestrian inhales
     */
    private final static VPoint RESET_EXHALATION_POSITION = null;

    // Constructors
    public TransmissionModelHealthStatus() {
        this(false, 0, false, 0, RESET_EXHALATION_POSITION);
    }

    public TransmissionModelHealthStatus(boolean isInfectious, double degreeOfExposure, boolean isBreathingIn, double respiratoryTimeOffset, VPoint exhalationStartPosition) {
        this.isInfectious = isInfectious;
        this.degreeOfExposure = degreeOfExposure;
        this.isBreathingIn = isBreathingIn;
        this.respiratoryTimeOffset = respiratoryTimeOffset;
        this.exhalationStartPosition = exhalationStartPosition;
    }

    public TransmissionModelHealthStatus(TransmissionModelHealthStatus other) {
        this.isInfectious = other.isInfectious();
        this.degreeOfExposure = other.getDegreeOfExposure();
        this.isBreathingIn = other.isInfectious();
        this.respiratoryTimeOffset = other.getRespiratoryTimeOffset();
        this.exhalationStartPosition = other.getExhalationStartPosition();
    }


    // Getter
    @Override
    public boolean isInfectious() {
        return isInfectious;
    }

    @Override
    public double getDegreeOfExposure() {
        return degreeOfExposure;
    }

    public boolean isBreathingIn() {
        return isBreathingIn;
    }

    public double getRespiratoryTimeOffset() {
        return respiratoryTimeOffset;
    }

    public VPoint getExhalationStartPosition() {
        return exhalationStartPosition;
    }

    void updateRespiratoryCycle(double simTimeInSec) {

    }

    public void incrementDegreeOfExposure(double deltaDegreeOfExposure) {
        this.degreeOfExposure += deltaDegreeOfExposure;
    }

    // Setter
    @Override
    public void setInfectious(boolean infectious) {
        isInfectious = infectious;
    }

    @Override
    public void setDegreeOfExposure(double degreeOfExposure) {
        this.degreeOfExposure = degreeOfExposure;
    }

    public void setBreathingIn(boolean breathingIn) {
        isBreathingIn = breathingIn;
    }

    public void setRespiratoryTimeOffset(double respiratoryTimeOffset) {
        this.respiratoryTimeOffset = respiratoryTimeOffset;
    }

    public void setExhalationStartPosition(VPoint exhalationStartPosition) {
        this.exhalationStartPosition = exhalationStartPosition;
    }

    // Methods

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
        return (!isBreathingIn && exhalationStartPosition == RESET_EXHALATION_POSITION);
    }

    public boolean isStartingInhalation() {
        return (isBreathingIn && !(exhalationStartPosition == RESET_EXHALATION_POSITION));
    }

    public void resetStartExhalationPosition() {
        exhalationStartPosition = RESET_EXHALATION_POSITION;
    }

}
