package org.vadere.state.health;

import org.vadere.util.geometry.shapes.VPoint;

import java.util.Objects;

public class ExtendedAirTransmissionModelHealthStatus extends AirTransmissionModelHealthStatus {

    boolean talking;

    boolean coughing;

    boolean sneezing;

    private int breathCounterCoughing;

    private int breathCounterSneezing;

    private int coughingEveryNthBreath;

    private int sneezingEveryNthBreath;

    public ExtendedAirTransmissionModelHealthStatus() {
        this(false, 0, RESET_EXHALATION_POSITION, false, false, false, -1, -1);
    }

    public ExtendedAirTransmissionModelHealthStatus(boolean breathingIn, double respiratoryTimeOffset, VPoint exhalationStartPosition, boolean talking, boolean coughing, boolean sneezing, int coughingEveryNthBreath, int sneezingEveryNthBreath) {
        super(breathingIn, respiratoryTimeOffset, exhalationStartPosition);
        this.talking = talking;
        this.coughing = coughing;
        this.sneezing = sneezing;
        this.breathCounterCoughing = 0;
        this.breathCounterSneezing = 0;
        this.coughingEveryNthBreath = coughingEveryNthBreath;
        this.sneezingEveryNthBreath = sneezingEveryNthBreath;
    }

    public ExtendedAirTransmissionModelHealthStatus(ExtendedAirTransmissionModelHealthStatus other) {
        super(other.isBreathingIn(), other.getRespiratoryTimeOffset(), other.getExhalationStartPosition());
        this.talking = other.isTalking();
        this.coughing = other.isCoughing();
        this.sneezing = other.isSneezing();
        this.breathCounterCoughing = other.breathCounterCoughing;
        this.breathCounterSneezing = other.breathCounterSneezing;
        this.coughingEveryNthBreath = other.coughingEveryNthBreath;
        this.sneezingEveryNthBreath = other.sneezingEveryNthBreath;
    }

    public boolean isTalking() {
        return talking;
    }

    public boolean isCoughing() { return coughing; }

    public boolean isSneezing() {
        return sneezing;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ExtendedAirTransmissionModelHealthStatus)) return false;
        if (!super.equals(obj)) return false;
        ExtendedAirTransmissionModelHealthStatus other = (ExtendedAirTransmissionModelHealthStatus) obj;
        return talking == other.talking && coughing == other.coughing && sneezing == other.sneezing;
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
