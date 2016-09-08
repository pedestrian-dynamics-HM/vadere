package org.vadere.simulator.projects.dataprocessing;

import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesPedestrianWaitingEndTimeProcessor extends AttributesProcessor {
    private VRectangle waitingArea = new VRectangle(0, 0, 1, 1);

    public VRectangle getWaitingArea() {
        return waitingArea;
    }
}
