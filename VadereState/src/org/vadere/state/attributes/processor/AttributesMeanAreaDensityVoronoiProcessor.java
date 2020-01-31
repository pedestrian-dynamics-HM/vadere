package org.vadere.state.attributes.processor;

/**
 * @author Marion Gödel
 *
 */

public class AttributesMeanAreaDensityVoronoiProcessor extends AttributesProcessor {
    private int pedestrianAreaDensityVoronoiProcessorId;

    public int getPedestrianAreaDensityVoronoiProcessorId() {
        return this.pedestrianAreaDensityVoronoiProcessorId;
    }

    public void setPedestrianAreaDensityVoronoiProcessorId(int pedestrianMeanAreaDensityVoronoiProcessorId) {
        checkSealed();
        this.pedestrianAreaDensityVoronoiProcessorId = pedestrianMeanAreaDensityVoronoiProcessorId;
    }
}
