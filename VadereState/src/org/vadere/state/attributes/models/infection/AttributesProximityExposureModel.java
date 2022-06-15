package org.vadere.state.attributes.models.infection;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;


/**
 * AttributesProximityExposureModel contains user-defined properties related to
 * the <code>ProximityExposureModel</code>.
 */
@ModelAttributeClass
public class AttributesProximityExposureModel extends AttributesExposureModel {

    /**
     * Within this radius around an infectious agent all other agents become exposed.
     */
    private double exposureRadius;

    public AttributesProximityExposureModel() {
        super();
        this.exposureRadius = 1;
    }

    // Getter
    public double getExposureRadius() {
        return exposureRadius;
    }
}
