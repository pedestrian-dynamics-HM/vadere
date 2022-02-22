package org.vadere.state.attributes.models.infection;

import org.vadere.state.attributes.Attributes;

/**
 * Attributes related to droplets that are shared among all droplets. These are not defined for each
 * instance of Droplets separately to keep Droplets lean.
 */
public class AttributesTransmissionModelDroplets extends Attributes {

    /**
     * Unit: 1/second
     */
    private double emissionFrequency;

    /**
     * Unit: meter
     */
    private double distanceOfSpread;

    /**
     * Unit: degree
     */
    private double angleOfSpreadInDeg;

    /**
     * Unit: second
     */
    private double lifeTime;

    /**
     * Unit: particles
     */
    private double pathogenLoad;

    /**
     * Describes the fraction infectious particles inhaled from droplets if the inhaling agent is located within
     * droplets.
     * Unit: 1 / inhalation
     */
    private double absorptionRate;

    public AttributesTransmissionModelDroplets() {
        this.emissionFrequency = 1.0 / 60.0;
        this.distanceOfSpread = 1.5;
        this.angleOfSpreadInDeg = 30;
        this.lifeTime = 1.5;
        this.pathogenLoad = 10000;
        this.absorptionRate = 0.1;
    }

    public double getEmissionFrequency() {
        return emissionFrequency;
    }

    public double getDistanceOfSpread() {
        return distanceOfSpread;
    }

    public double getAngleOfSpreadInDeg() {
        return angleOfSpreadInDeg;
    }

    public double getLifeTime() {
        return lifeTime;
    }

    public double getPathogenLoad() {
        return pathogenLoad;
    }

    public double getAbsorptionRate() {
        return absorptionRate;
    }

    public void setEmissionFrequency(double emissionFrequency) {
        this.emissionFrequency = emissionFrequency;
    }

    public void setDistanceOfSpread(double distanceOfSpread) {
        this.distanceOfSpread = distanceOfSpread;
    }

    public void setAngleOfSpreadInDeg(double angleOfSpreadInDeg) {
        this.angleOfSpreadInDeg = angleOfSpreadInDeg;
    }

    public void setLifeTime(double lifeTime) {
        this.lifeTime = lifeTime;
    }

    public void setPathogenLoad(double pathogenLoad) {
        this.pathogenLoad = pathogenLoad;
    }

    public void setAbsorptionRate(double absorptionRate) {
        this.absorptionRate = absorptionRate;
    }

}
