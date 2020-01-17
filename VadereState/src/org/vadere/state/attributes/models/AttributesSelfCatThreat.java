package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;

public class AttributesSelfCatThreat extends Attributes {

    AttributesOSM attributesLocomotion = new AttributesOSM();
    double probabilityInGroupMembership = 0.0;

    public AttributesOSM getAttributesLocomotion() {
        return attributesLocomotion;
    }
    public double getProbabilityInGroupMembership() { return probabilityInGroupMembership; }

}
