package org.vadere.simulator.models.dsm;

import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Kevin Becker
 */
public class DSMStep {

    private final int pedestrianId;
    private final double startTime;
    private final double endTime;
    private final VPoint startPosition;
    private final VPoint endPosition;
    private final int targetId;

    public DSMStep(int pedestrianId, double startTime, double endTime, VPoint startPosition, VPoint endPosition, int targetId) {
        this.pedestrianId = pedestrianId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.targetId = targetId;
    }

    public int getPedestrianId() {
        return pedestrianId;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public VPoint getStartPosition() {
        return startPosition;
    }

    public VPoint getEndPosition() {
        return endPosition;
    }

    public int getTargetId() {
        return targetId;
    }

    @Override
    public String toString() {
        return pedestrianId + " " + startTime + " " + endTime + " " + startPosition.getX() + " " + startPosition.getY() + " " + endPosition.getX() + " " + endPosition.getY() + " " + targetId;
    }
}
