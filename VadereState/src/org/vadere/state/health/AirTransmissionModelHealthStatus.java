package org.vadere.state.health;

import org.vadere.util.geometry.shapes.VPoint;

import java.util.Objects;

/**
 * AirTransmissionModelHealthStatus that is used in combination with the
 * <code>AirTransmissionModel</code>.
 */
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

    boolean talking;

    boolean coughing;

    boolean sneezing;

    private int breathCounterCoughing;

    private int breathCounterSneezing;

    private int coughingEveryNthBreath;

    private int sneezingEveryNthBreath;

    /*
     * reset value for simulation periods during which pedestrian inhales
     */
    private final static VPoint RESET_EXHALATION_POSITION = null;


    // Constructors
    public AirTransmissionModelHealthStatus() {
        this(false, 0, RESET_EXHALATION_POSITION, false, false, false, -1, -1);
    }

    public AirTransmissionModelHealthStatus(boolean breathingIn, double respiratoryTimeOffset, VPoint exhalationStartPosition, boolean talking, boolean coughing, boolean sneezing, int coughingEveryNthBreath, int sneezingEveryNthBreath) {
        super();
        this.breathingIn = breathingIn;
        this.respiratoryTimeOffset = respiratoryTimeOffset;
        this.exhalationStartPosition = exhalationStartPosition;
        this.talking = talking;
        this.coughing = coughing;
        this.sneezing = sneezing;
        this.breathCounterCoughing = 0;
        this.breathCounterSneezing = 0;
        this.coughingEveryNthBreath = coughingEveryNthBreath;
        this.sneezingEveryNthBreath = sneezingEveryNthBreath;
    }

    public AirTransmissionModelHealthStatus(AirTransmissionModelHealthStatus other) {
        super(other.isInfectious(), other.getDegreeOfExposure());
        this.breathingIn = other.isBreathingIn();
        this.respiratoryTimeOffset = other.getRespiratoryTimeOffset();
        this.exhalationStartPosition = other.getExhalationStartPosition();
        this.talking = other.talking;
        this.coughing = other.coughing;
        this.sneezing = other.sneezing;
        this.breathCounterCoughing = other.breathCounterCoughing;
        this.breathCounterSneezing = other.breathCounterSneezing;
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

    public boolean isTalking() {
        return talking;
    }

    public boolean isCoughing() { return coughing; }

    public boolean isSneezing() {
        return sneezing;
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

    public void setTalking(boolean talking) {
        this.talking = talking;
    }

    public void setCoughing(boolean coughing) {
        this.coughing = coughing;
    }

    public void setSneezing(boolean sneezing) {
        this.sneezing = sneezing;
    }

    public void setCoughingEveryNthBreath(int coughingEveryNthBreath) {
        this.coughingEveryNthBreath = coughingEveryNthBreath;
    }

    public void setSneezingEveryNthBreath(int sneezingEveryNthBreath) {
        this.sneezingEveryNthBreath = sneezingEveryNthBreath;
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
    @Override
    public AirTransmissionModelHealthStatus clone() {
        return new AirTransmissionModelHealthStatus(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AirTransmissionModelHealthStatus)) return false;
        if (!super.equals(obj)) return false;
        AirTransmissionModelHealthStatus other = (AirTransmissionModelHealthStatus) obj;
        return breathingIn == (other.breathingIn && Double.compare(other.respiratoryTimeOffset, respiratoryTimeOffset) == 0
                && Objects.equals(exhalationStartPosition, other.exhalationStartPosition)
                && talking == other.talking && coughing == other.coughing && sneezing == other.sneezing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(breathingIn, respiratoryTimeOffset, exhalationStartPosition);
    }

    /**
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


    public boolean isCoughingNow() {
        return (breathCounterCoughing == coughingEveryNthBreath);
    }

    public void incrementBreathCounterCoughing() {
        breathCounterCoughing++;
    }

    public void resetBreathCounterCoughing() {
        breathCounterCoughing = 0;
    }

    public boolean isSneezingNow() {
        return (breathCounterSneezing == sneezingEveryNthBreath);
    }

    public void incrementBreathCounterSneezing() {
        breathCounterSneezing++;
    }

    public void resetBreathCounterSneezing() {
        breathCounterSneezing = 0;
    }
}
