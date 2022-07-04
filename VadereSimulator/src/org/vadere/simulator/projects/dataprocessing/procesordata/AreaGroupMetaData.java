package org.vadere.simulator.projects.dataprocessing.procesordata;

import org.vadere.simulator.models.groups.Group;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;
import java.util.List;
import java.util.Optional;

public class AreaGroupMetaData {

    final private Group group;
    private int sizeInArea;
    final protected List<Pedestrian> membersInArea;
    private VPoint centroid;

    private boolean centroidInArea;
    private int totalPedestriansInArea;
    private double simTime;
    private long pedestriansLost;

    public AreaGroupMetaData(Group group, int sizeInArea, List<Pedestrian> membersInArea, int totalPedestriansInArea,
                             long pedestriansLost) {
        this.group = group;
        this.sizeInArea = sizeInArea;
        this.membersInArea = membersInArea;
        this.totalPedestriansInArea = totalPedestriansInArea;
        this.simTime = 0;
        this.pedestriansLost = pedestriansLost;
    }

    public double getSimTime() {
        return simTime;
    }

    public void setSimTime(double simTime) {
        this.simTime = simTime;
    }

    public int getTotalPedestriansInArea() {
        return totalPedestriansInArea;
    }

    public void setTotalPedestriansInArea(int totalPedestriansInArea) {
        this.totalPedestriansInArea = totalPedestriansInArea;
    }

    public Group getGroup() {
        return group;
    }

    public int getSizeInArea() {
        return sizeInArea;
    }

    public void setSizeInArea(int sizeInArea) {
        this.sizeInArea = sizeInArea;
    }

    public List<Pedestrian> getMembersInArea() {
        return membersInArea;
    }

    public Optional<VPoint> getCentroid() {
        return Optional.ofNullable(centroid);
    }

    public Optional<Boolean> isCentroidInArea() {
        return Optional.of(centroidInArea);
    }


    public void setCentroid(VPoint centroid) {
        this.centroid = centroid;
    }

    public void setCentroidInArea(boolean centroidInArea) {
        this.centroidInArea = centroidInArea;
    }

    public long getPedestriansLost() {
        return pedestriansLost;
    }

    public void setPedestriansLost(int pedestriansLost) {
        this.pedestriansLost = pedestriansLost;
    }
}
