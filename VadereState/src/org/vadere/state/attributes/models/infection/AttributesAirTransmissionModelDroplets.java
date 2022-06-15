package org.vadere.state.attributes.models.infection;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesDroplets;
import org.vadere.state.scenario.Droplets;

/**
 * Attributes related to droplets that are shared among all droplets. These are not defined for each
 * instance of {@link Droplets} separately to keep the class lean.
 */
public class AttributesAirTransmissionModelDroplets extends Attributes {

    /**
     * Describes how often {@link Droplets} are emitted by an infectious pedestrian.
     * Unit: 1 / second
     */
    private double emissionFrequency;

    /**
     * Describes the shape of {@link AttributesDroplets}: Radius of the circular segment.
     * Unit: meter
     */
    private double distanceOfSpread;

    /**
     * Describes the shape of {@link AttributesDroplets}: Angle of the circular segment.
     * Unit: degree
     */
    private double angleOfSpreadInDeg;

    /**
     * Describes the persistence of {@link Droplets}.
     * Unit: second
     */
    private double lifeTime;

    /**
     * Describes the pathogen load within {@link Droplets}. It remains constant over
     * {@link #lifeTime}.
     * Unit: particles
     */
    private double pathogenLoad;

    /**
     * Describes the fraction infectious particles inhaled from droplets if the inhaling agent is located within
     * droplets.
     * Unit: 1 / inhalation
     */
    private double absorptionRate;

    public AttributesAirTransmissionModelDroplets() {
        this.emissionFrequency = 1.0 / 60.0;
        this.distanceOfSpread = 1.5;
        this.angleOfSpreadInDeg = 30;
        this.lifeTime = 1.5;
        this.pathogenLoad = 10000;
        this.absorptionRate = 0.1;
    }

    public AttributesAirTransmissionModelDroplets(double emissionFrequency, double distanceOfSpread, double angleOfSpreadInDeg, double lifeTime, double pathogenLoad, double absorptionRate) {
        this.emissionFrequency = emissionFrequency;
        this.distanceOfSpread = distanceOfSpread;
        this.angleOfSpreadInDeg = angleOfSpreadInDeg;
        this.lifeTime = lifeTime;
        this.pathogenLoad = pathogenLoad;
        this.absorptionRate = absorptionRate;
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

    protected void setLifeTime(double lifeTime) {
        this.lifeTime = lifeTime;
    }

    protected void setAngleOfSpreadInDeg(double angleOfSpreadInDeg) {
        this.angleOfSpreadInDeg = angleOfSpreadInDeg;
    }
}
