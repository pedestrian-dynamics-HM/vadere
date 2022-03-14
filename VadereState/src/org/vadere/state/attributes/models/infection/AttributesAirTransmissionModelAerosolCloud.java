package org.vadere.state.attributes.models.infection;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.AerosolCloud;

/**
 * Attributes related to aerosol clouds that are shared among all aerosol clouds. These are not defined for each
 * instance of {@link AerosolCloud} separately to keep the class lean.
 * The frequency of occurrence is not defined here because {@link AerosolCloud}s (if considered in the exposure model)
 * are directly linked to the respiratory cycle defined in {@link AttributesAirTransmissionModel}.
 */
public class AttributesAirTransmissionModelAerosolCloud extends Attributes {

    /**
     * Describes exponential decay of pathogen load within an aerosol cloud;
     * Unit: seconds
     */
    private double halfLife;

    /**
     * Describes initial spatial extent of aerosol clouds. The aerosol clouds have all the same initial
     * circular extent (in the two-dimensional model). In 3D, we actually imagine them as spheres.
     * Unit: meter
     */
    private double initialRadius;

    /**
     * Describes the amount of pathogen in an aerosol cloud immediately after exhalation.
     * Indirectly describes the infectious pedestrian's infectiousness; is to be reduced if aerosol emission
     * is mitigated, e.g. by masks.
     * Unit: particles / exhalation
     */
    private double initialPathogenLoad;

    /**
     * Describes constant dispersion (over time), i.e. the spatial spread over time due to local air movement.
     * This increases the radius of aerosol clouds equally in all directions (three-dimensional).
     * Unit: meter / second
     */
    private double airDispersionFactor;

    /**
     * Describes dispersion depending on agents, i.e. the spatial spread of aerosol clouds caused by moving agents
     * (which results temporally in local air movement). Each agent that passes a cloud with speed > 0 m / s contributes
     * to the dispersion of the cloud: deltaRadius = pedestrianSpeed * pedestrianDispersionWeight
     * This increases the radius of aerosol clouds equally in all directions (three-dimensional).
     * Unit: 1
     */
    private double pedestrianDispersionWeight;

    /**
     * Describes how much pathogen carried by aerosols is absorbed per inhalation; is to be reduced if aerosol
     * inhalation is mitigated, e.g. by masks.
     * Unit: 1 / inhalation (can also be interpreted as m^3 / inhalation)
     */
    private double absorptionRate;

    public AttributesAirTransmissionModelAerosolCloud() {
        this.halfLife = 600;
        this.initialRadius = 1.5;
        this.initialPathogenLoad = 10000;
        this.airDispersionFactor = 0;
        this.pedestrianDispersionWeight = 0.0125;
        this.absorptionRate = 0.0005;
    }

    public AttributesAirTransmissionModelAerosolCloud(double aerosolCloudHalfLife, double aerosolCloudInitialRadius, double initialPathogenLoad, double airDispersionFactor, double pedestrianDispersionWeight, double absorptionRate) {
        this.halfLife = aerosolCloudHalfLife;
        this.initialRadius = aerosolCloudInitialRadius;
        this.initialPathogenLoad = initialPathogenLoad;
        this.airDispersionFactor = airDispersionFactor;
        this.pedestrianDispersionWeight = pedestrianDispersionWeight;
        this.absorptionRate = absorptionRate;
    }

    // Getter

    public double getHalfLife() {
        return halfLife;
    }

    public double getInitialRadius() {
        return initialRadius;
    }

    public double getInitialPathogenLoad() {
        return initialPathogenLoad;
    }

    public double getAirDispersionFactor() {
        return airDispersionFactor;
    }

    public double getPedestrianDispersionWeight() {
        return pedestrianDispersionWeight;
    }

    public double getAbsorptionRate() {
        return absorptionRate;
    }

    protected void setHalfLife(double halfLife) {
        this.halfLife = halfLife;
    }

    protected void setAirDispersionFactor(double airDispersionFactor) {
        this.airDispersionFactor = airDispersionFactor;
    }

    protected void setPedestrianDispersionWeight(double pedestrianDispersionWeight) {
        this.pedestrianDispersionWeight = pedestrianDispersionWeight;
    }
}
