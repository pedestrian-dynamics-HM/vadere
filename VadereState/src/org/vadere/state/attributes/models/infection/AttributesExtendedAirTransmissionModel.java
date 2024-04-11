package org.vadere.state.attributes.models.infection;

import java.util.ArrayList;
import java.util.List;

public class AttributesExtendedAirTransmissionModel extends AttributesAirTransmissionModel {

    private ArrayList<AttributesExtendedExposureModelSourceParameters> exposureModelSourceParameters;
    private AttributesExtendedAirTransmissionModelAerosolCloud aerosolCloudParameters;

    public AttributesExtendedAirTransmissionModel() {
        this.aerosolCloudParameters = new AttributesExtendedAirTransmissionModelAerosolCloud();
        this.exposureModelSourceParameters = new ArrayList<>(List.of(new AttributesExtendedExposureModelSourceParameters()));
    }

    public int getAerosolCloudPathogenLoadMultiplierTalking() {
        return aerosolCloudParameters.getPathogenLoadMultiplierTalking();
    }

    public int getAerosolCloudPathogenLoadMultiplierCoughing() {
        return aerosolCloudParameters.getPathogenLoadMultiplierCoughing();
    }

    public int getAerosolCloudPathogenLoadMultiplierSneezing() {
        return aerosolCloudParameters.getPathogenLoadMultiplierSneezing();
    }

    public ArrayList<AttributesExtendedExposureModelSourceParameters> getExtendedExposureModelSourceParameters() {
        return exposureModelSourceParameters;
    }
}
