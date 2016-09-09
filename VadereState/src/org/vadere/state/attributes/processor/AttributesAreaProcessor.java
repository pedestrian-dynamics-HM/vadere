package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesAreaProcessor extends AttributesProcessor {
    private VRectangle measurementArea = new VRectangle(0, 0, 1, 1);

    public VRectangle getMeasurementArea() {
        return this.measurementArea;
    }
}
