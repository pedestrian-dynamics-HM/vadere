package org.vadere.state.attributes.models.infection;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;



@ModelAttributeClass
public class AttributesThresholdResponseModel extends AttributesDoseResponseModel {

    /**
     * Degree of exposure at which a pedestrian's probability of infection changes from 0 to 1.
     */
    private double exposureToInfectedThreshold;

    public AttributesThresholdResponseModel() {
        this.exposureToInfectedThreshold = 1;
    }

    public AttributesThresholdResponseModel(double exposureToInfectedThreshold) {
        this.exposureToInfectedThreshold = exposureToInfectedThreshold;
    }

    public double getExposureToInfectedThreshold() {
        return exposureToInfectedThreshold;
    }
}
