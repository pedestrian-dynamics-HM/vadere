package org.vadere.state.attributes.processor;

/**
 * @author Maxim Dudin
 *
 */

public class AttributesPedestrianNearbyProcessor extends AttributesProcessor {
    private  double maxDistanceForANearbyPedestrian = 1.5;

    public double getMaxDistanceForANearbyPedestrian() {
        return maxDistanceForANearbyPedestrian;
    }
}
