package org.vadere.state.events.types;

import java.lang.reflect.Field;

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

    public EventTimeframe() {
        this(0, 0, false, 0);
    }

    public EventTimeframe(double startTime, double endTime, boolean repeat, double waitTimeBetweenRepetition) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeat = repeat;
        this.waitTimeBetweenRepetition = waitTimeBetweenRepetition;
    }

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

    @Override
    public String toString() {
        String string = "EventTimeframe:\n";
        string += String.format("  startTime: %f\n", startTime);
        string += String.format("  endTime: %f\n", endTime);
        string += String.format("  repeat: %b\n", repeat);
        string += String.format("  waitTimeBetweenRepetition: %f\n", waitTimeBetweenRepetition);

        return string;
    }

}
