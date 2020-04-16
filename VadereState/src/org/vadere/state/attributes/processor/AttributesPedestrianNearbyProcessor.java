package org.vadere.state.attributes.processor;

/**
 * @author Maxim Dudin
 *
 */

public class AttributesPedestrianNearbyProcessor extends AttributesProcessor {
    private double maxDistanceForANearbyPedestrian = 1.5;
    private int sampleEveryNthStep = 1;

    public double getMaxDistanceForANearbyPedestrian() {
        return maxDistanceForANearbyPedestrian;
    }

    public int getSampleEveryNthStep() {
        return sampleEveryNthStep;
    }
}
