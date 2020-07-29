package org.vadere.state.attributes.processor;

/**
 * @author Daniel Lehmberg
 */

public class AttributesAreaDensityCountingProcessor extends AttributesAreaProcessor {
    boolean countsInsteadOfDensity = true;
    private int measurementAreaId = -1;

    public boolean getCountsInsteadOfDensity()
    {
        return countsInsteadOfDensity;
    }

    public void setCountsInsteadOfDensity(boolean countsInsteadOfDensity)
    {
        this.countsInsteadOfDensity = countsInsteadOfDensity;
    }

    public int getMeasurementAreaId() {
        return this.measurementAreaId;
    }

    public void setMeasurementAreaId(int measurementAreaId) {
        checkSealed();
        this.measurementAreaId = measurementAreaId;
    }

}
