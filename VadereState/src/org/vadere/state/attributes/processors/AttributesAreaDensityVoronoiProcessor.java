package org.vadere.state.attributes.processors;

import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesAreaDensityVoronoiProcessor extends AttributesAreaProcessor {
    private VRectangle voronoiArea = new VRectangle(0, 0, 1, 1);

    public VRectangle getVoronoiArea() {
        return this.voronoiArea;
    }
}
