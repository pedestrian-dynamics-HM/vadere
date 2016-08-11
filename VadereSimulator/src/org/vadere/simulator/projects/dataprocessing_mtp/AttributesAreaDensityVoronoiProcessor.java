package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesAreaDensityVoronoiProcessor extends AttributesAreaProcessor {
    private VRectangle voronoiArea = new VRectangle(0, 0, 1, 1);

    public VRectangle getVoronoiArea() {
        return this.voronoiArea;
    }
}
