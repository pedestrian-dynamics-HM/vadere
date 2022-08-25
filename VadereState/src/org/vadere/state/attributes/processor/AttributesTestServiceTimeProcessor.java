package org.vadere.state.attributes.processor;

public class AttributesTestServiceTimeProcessor extends AttributesTestProcessor{
    private Double upperBound;
    private Double lowerBound;

    public Double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Double upperBound) {
        checkSealed();
        this.upperBound = upperBound;
    }

    public Double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Double lowerBound) {
        checkSealed();
        this.lowerBound = lowerBound;
    }
}
