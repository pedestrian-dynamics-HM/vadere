package org.vadere.simulator.projects.dataprocessing.procesordata;

import org.vadere.util.geometry.PointPositioned;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

public class PedestrianIdDuration implements PointPositioned {

    private int pedestrianId;
    private double duration;

    private VRectangle position;

    public PedestrianIdDuration(int pedestrianId, double duration, VRectangle position) {
        this.pedestrianId = pedestrianId;
        this.duration = duration;
        this.position = position;
    }

    public int getPedestrianId() {
        return pedestrianId;
    }

    public void setPedestrianId(int pedestrianId) {
        this.pedestrianId = pedestrianId;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    @Override
    public VPoint getPosition() {
        return new VPoint(position.getCenterX(), position.getCenterY());
    }
}
