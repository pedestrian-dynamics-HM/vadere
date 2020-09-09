package org.vadere.state.simulation;

import org.jetbrains.annotations.Nullable;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Store the last foot steps of an agent to calculate the agent's average speed during simulation.
 */
public class FootstepHistory {

    // Variables
    private int capacity;
    private ArrayList<FootStep> footSteps;

    // Constructors
    public FootstepHistory() {
        this(10);
    }
    public FootstepHistory(int capacity) {
        this.capacity = capacity;
        this.footSteps = new ArrayList<>(capacity);
    }

    // Getters
    public int getCapacity() { return capacity; }
    public ArrayList<FootStep> getFootSteps() { return footSteps; }
    public int size() { return footSteps.size(); }

    // Methods
    public boolean add(FootStep footStep) {
        if (footSteps.size() >= capacity) {
            footSteps.remove(0);
        }

        boolean successful = footSteps.add(footStep);

        return successful;
    }

	public void removeLast() {
		assert !footSteps.isEmpty();
		footSteps.remove(footSteps.size()-1);
	}

    public double getAverageSpeedInMeterPerSecond() {
        double speed = Double.NaN;

        if (footSteps.size() > 0) {
            // Speed is length divided by time.
            double distance =  footSteps.stream().mapToDouble(footStep -> footStep.length()).sum();
            // This approach works also if "footSteps.size() == 1"
            double time = getYoungestFootStep().getEndTime() - getOldestFootStep().getStartTime();

            speed = distance / time;
        }

        return speed;
    }

    public FootStep getOldestFootStep() {
        FootStep oldestFootStep = null;

        if (footSteps.size() > 0) {
            oldestFootStep = footSteps.get(0);
        }

        return oldestFootStep;
    }

    /**
     *  Heading based on  TraCI angle
     *  * - measured in degree
     *  * - 0 is headed north
     *  * - clockwise orientation (i.e. 90 heads east, 180 heads south, etc.)
     *  * - range from 0 to 360.
     *
     * @return Heading angle based on last FootStep.
     */
    public double getNorthBoundHeadingAngleDeg(){
        return getNorthBoundHeadingAngle(1, true);
    }

    public double getNorthBoundHeadingAngleRad(){
        return getNorthBoundHeadingAngle(1, false);
    }

    public double getNorthBoundHeadingAngle(int histLength, boolean degree){
        if (footSteps.size() < histLength)
            return 0.0; // not enough data. Return North heading.

        VPoint currentLocation = footSteps.get(footSteps.size() -1).getEnd();
        VPoint pastLocation = footSteps.get(footSteps.size() - histLength).getStart();
        Vector2D heading = new Vector2D(currentLocation.x - pastLocation.x, currentLocation.y - pastLocation.y);
        if (Math.abs(heading.getLength() -0.0) < 0.0001){
            //Footstep to small
            return 0.0; // assume North heading
        }

        // TraCI Angle defined as Clockwise with North as 0 deg.
        // clockwise: | 2PI - angle |
        // 0 at North: + PI
        // mod 2PI for [0, 2PI]
        double angel = (Math.abs(2*Math.PI - heading.angleToZero()) + 0.5*Math.PI) % (2* Math.PI); // angle with y-axis

        if (degree)
            return  angel*(180/Math.PI);
        return angel;
    }

    @Nullable
    public FootStep getYoungestFootStep() {
        FootStep youngestFootStep = null;

        if (footSteps.size() > 0) {
            youngestFootStep = footSteps.get(footSteps.size() - 1);
        }

        return youngestFootStep;
    }

    @Override
    public String toString() {
        String footStepPrefix = String.format("Last Footseps (%d): ", footSteps.size());

        String footStepString = footSteps.stream().map(footStep -> footStep.toString()).collect(Collectors.joining(" -> "));

        return footStepPrefix + footStepString;
    }
}
