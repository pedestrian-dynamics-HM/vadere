package org.vadere.state.attributes.processor;

public class AttributesPathogenConcentrationProcessor extends AttributesProcessor {

    double gridResolution = 0.5;
    double timeResolution = 4;

    public double getGridResolution() {
        return gridResolution;
    }

    public double getTimeResolution() {
        return timeResolution;
    }

    public void setGridResolution(double gridResolution) {
        checkSealed();
        this.gridResolution = gridResolution;
    }
}
