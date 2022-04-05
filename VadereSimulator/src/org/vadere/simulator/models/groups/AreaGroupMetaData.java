package org.vadere.simulator.models.groups;

import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.List;

public class AreaGroupMetaData {

    final private Group group;
    final private int sizeInArea;
    final protected List<Pedestrian> membersInArea;
    private VPoint centroid;
    private boolean centroidInArea;

    public AreaGroupMetaData(Group group, int sizeInArea, List<Pedestrian> membersInArea) {
        this.group = group;
        this.sizeInArea = sizeInArea;
        this.membersInArea = membersInArea;
    }

    public Group getGroup() {
        return group;
    }

    public int getSizeInArea() {
        return sizeInArea;
    }

    public List<Pedestrian> getMembersInArea() {
        return membersInArea;
    }

    public VPoint getCentroid() {
        return centroid;
    }

    public boolean isCentroidInArea() {
        return centroidInArea;
    }


    public void setCentroid(VPoint centroid) {
        this.centroid = centroid;
    }

    public void setCentroidInArea(boolean centroidInArea) {
        this.centroidInArea = centroidInArea;
    }
}
