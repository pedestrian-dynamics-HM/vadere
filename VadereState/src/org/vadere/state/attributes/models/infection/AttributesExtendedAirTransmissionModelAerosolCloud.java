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

    public AttributesExtendedAirTransmissionModelAerosolCloud() {
        super();

        this.pathogenLoadMultiplierTalking = 10;
        this.pathogenLoadMultiplierCoughing = 20;
        this.pathogenLoadMultiplierSneezing = 100;
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

}
