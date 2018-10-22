package org.vadere.state.attributes.processor;

import org.vadere.geometry.shapes.VRectangle;

/**
 * @author Mario Teixeira Parente
 */

public class AttributesAreaProcessor extends AttributesProcessor {
	private VRectangle measurementArea = new VRectangle(0, 0, 1, 1);

    public VRectangle getMeasurementArea() {
        return this.measurementArea;
    }

    public void setMeasurementArea(VRectangle measurementArea) {
        checkSealed();
        this.measurementArea = measurementArea;
    }
}
