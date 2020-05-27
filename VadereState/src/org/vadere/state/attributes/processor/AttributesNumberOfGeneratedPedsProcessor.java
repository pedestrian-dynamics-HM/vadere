package org.vadere.state.attributes.processor;

public class AttributesNumberOfGeneratedPedsProcessor extends AttributesProcessor {

    private double startTime = 0.0;
    private double endTime = -1; // -1 until simulation finished


    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        if (endTime >= startTime - 1e-7)
            return endTime;
        else
            return Double.MAX_VALUE;

    }
}
