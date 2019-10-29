package org.vadere.state.psychology.perception.types;

/**
 * A timeframe in which one ore more stimuli can occur.
 *
 * This information is required by a stimulus controller
 * which injects the actual stimuli into the simulation loop.
 */
public class Timeframe {

    private double startTime;
    private double endTime;

    private boolean repeat;
    private double waitTimeBetweenRepetition;

    public Timeframe() {
        this(0, 0, false, 0);
    }

    public Timeframe(double startTime, double endTime, boolean repeat, double waitTimeBetweenRepetition) {
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
        String string = "Timeframe:\n";
        string += String.format("  startTime: %f\n", startTime);
        string += String.format("  endTime: %f\n", endTime);
        string += String.format("  repeat: %b\n", repeat);
        string += String.format("  waitTimeBetweenRepetition: %f\n", waitTimeBetweenRepetition);

        return string;
    }

}
