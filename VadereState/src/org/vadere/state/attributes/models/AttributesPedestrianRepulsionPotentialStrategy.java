package org.vadere.state.attributes.models;

public class AttributesPedestrianRepulsionPotentialStrategy extends AttributesCombinedPotentialStrategy {

    private double repulsionFactor = 1.6444;
    private double repulsionIntercept  = 0.4845;
    private double minDistance = 1.25;
    private double maxDistance = 2.0;

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public double getRepulsionIntercept() {
        return repulsionIntercept;
    }

    public void setRepulsionIntercept(double repulsionIntercept) {
        this.repulsionIntercept = repulsionIntercept;
    }

    public double getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(double minDistance) {
        this.minDistance = minDistance;
    }

    public double getRepulsionFactor() {
        return repulsionFactor;
    }

    public void setRepulsionFactor(double repulsionFactor) {
        this.repulsionFactor = repulsionFactor;
    }
}
