package org.vadere.state.events.types;

/**
 * A timeframe in which events occur.
 *
 * This information is required by an event controller which raises the actual events during simulation.
 */
public class EventTimeframe {

    private double startTime;
    private double endTime;

    private boolean repeat;
    private double waitTimeBetweenRepetition;

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public double getWaitTimeBetweenRepetition() {
        return waitTimeBetweenRepetition;
    }

    public void setWaitTimeBetweenRepetition(double waitTimeBetweenRepetition) {
        this.waitTimeBetweenRepetition = waitTimeBetweenRepetition;
    }

}
