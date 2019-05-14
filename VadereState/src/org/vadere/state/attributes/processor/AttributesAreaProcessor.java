package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 * @author Stefan Schuhbäck (major update)
 */

public class AttributesAreaProcessor extends AttributesProcessor {
	private int measurementAreaId = -1;

    public int getMeasurementAreaId() {
        return this.measurementAreaId;
    }

    public void setMeasurementAreaId(int measurementAreaId) {
        checkSealed();
        this.measurementAreaId = measurementAreaId;
    }
}
