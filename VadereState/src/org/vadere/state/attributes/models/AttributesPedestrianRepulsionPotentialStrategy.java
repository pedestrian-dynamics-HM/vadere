package org.vadere.state.attributes.models;

public class AttributesPedestrianRepulsionPotentialStrategy extends AttributesCombinedPotentialStrategy {

    /* Parameter values, see Mayr, Koester: Social distancing with the Optimal Steps Model*/

    private double personalSpaceWidthFactor = 1.6444;
    private double personalSpaceWidthIntercept = 0.4845;
    private double socialDistanceLowerBound = 1.25;
    private double socialDistanceUpperBound = 2.0;
    private double personalSpaceStrength = 850.0;

    public double getSocialDistanceUpperBound() {
        return socialDistanceUpperBound;
    }

    public void setSocialDistanceUpperBound(double socialDistanceUpperBound) {
        this.socialDistanceUpperBound = socialDistanceUpperBound;
    }

    public double getPersonalSpaceWidthIntercept() {
        return personalSpaceWidthIntercept;
    }

    public void setPersonalSpaceWidthIntercept(double personalSpaceWidthIntercept) {
        this.personalSpaceWidthIntercept = personalSpaceWidthIntercept;
    }

    public double getSocialDistanceLowerBound() {
        return socialDistanceLowerBound;
    }

    public void setSocialDistanceLowerBound(double socialDistanceLowerBound) {
        this.socialDistanceLowerBound = socialDistanceLowerBound;
    }

    public double getPersonalSpaceWidthFactor() {
        return personalSpaceWidthFactor;
    }

    public void setPersonalSpaceWidthFactor(double personalSpaceWidthFactor) {
        this.personalSpaceWidthFactor = personalSpaceWidthFactor;
    }

    public double getPersonalSpaceStrength() {
        return personalSpaceStrength;
    }

    public void setPersonalSpaceStrength(double personalSpaceStrength) {
        this.personalSpaceStrength = personalSpaceStrength;
    }
}
