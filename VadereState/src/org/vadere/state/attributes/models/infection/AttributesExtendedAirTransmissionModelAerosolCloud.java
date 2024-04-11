package org.vadere.state.attributes.models.infection;

public class AttributesExtendedAirTransmissionModelAerosolCloud extends AttributesAirTransmissionModelAerosolCloud {

    /**
     * Describes the number the initial pathogen load is multiplied with if the pedestrian is talking.
     * Unit: 1
     */
    private int pathogenLoadMultiplierTalking;

    /**
     * Describes the number the initial pathogen load is multiplied with if the pedestrian is coughing.
     * Unit: 1
     */
    private int pathogenLoadMultiplierCoughing;

    /**
     * Describes the number the initial pathogen load is multiplied with if the pedestrian is sneezing.
     * Unit: 1
     */
    private int pathogenLoadMultiplierSneezing;

    /**
     * Describes the speed of the wind shifting the AerosolClouds
     * Unit: cm/s
     */
    private double windSpeed;

    /**
     * Describes the direction of the wind
     * Unit: point of the compass
     */
    private WindDirection windDirection;

    public AttributesExtendedAirTransmissionModelAerosolCloud() {
        super();

        this.pathogenLoadMultiplierTalking = 10;
        this.pathogenLoadMultiplierCoughing = 20;
        this.pathogenLoadMultiplierSneezing = 100;
        this.windSpeed = 10;
        this.windDirection = WindDirection.N;
    }

    public AttributesExtendedAirTransmissionModelAerosolCloud(double aerosolCloudHalfLife,
                                                              double aerosolCloudInitialRadius, int initialPathogenLoad,
                                                              double airDispersionFactor,
                                                              double pedestrianDispersionWeight, double absorptionRate,
                                                              int pathogenLoadMultiplierTalking,
                                                              int pathogenLoadMultiplierCoughing,
                                                              int pathogenLoadMultiplierSneezing) {

        super(aerosolCloudHalfLife, aerosolCloudInitialRadius, initialPathogenLoad, airDispersionFactor,
                pedestrianDispersionWeight, absorptionRate);
        this.pathogenLoadMultiplierTalking = pathogenLoadMultiplierTalking;
        this.pathogenLoadMultiplierCoughing = pathogenLoadMultiplierCoughing;
        this.pathogenLoadMultiplierSneezing = pathogenLoadMultiplierSneezing;
    }

    public int getPathogenLoadMultiplierTalking() { return pathogenLoadMultiplierTalking; }

    public int getPathogenLoadMultiplierCoughing() { return pathogenLoadMultiplierCoughing; }

    public int getPathogenLoadMultiplierSneezing() { return pathogenLoadMultiplierSneezing; }

    public double getWindSpeed() {
        return windSpeed;
    }

    public double getWindDirection()  {
        return windDirection.angle;
    }

    public enum WindDirection {
        N(Math.PI * 1/2),
        NO(Math.PI * 1/4),
        O(0),
        SO(Math.PI * 7/4),
        S(Math.PI * 3/2),
        SW(Math.PI * 5/4),
        W(Math.PI),
        NW(Math.PI * 3/4);

        final double angle;

        WindDirection(double angle) {
            this.angle = angle;
        }
    }

}
