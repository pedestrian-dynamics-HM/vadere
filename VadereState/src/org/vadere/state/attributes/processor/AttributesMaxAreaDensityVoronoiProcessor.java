package org.vadere.state.attributes.processor;

/**
 * @author Marion Gödel
 *
 */

public class AttributesMaxAreaDensityVoronoiProcessor extends AttributesProcessor {
    private int pedestrianAreaDensityVoronoiProcessorId;

    public int getPedestrianAreaDensityVoronoiProcessorId() {
        return this.pedestrianAreaDensityVoronoiProcessorId;
    }

    public void setPedestrianAreaDensityVoronoiProcessorId(int pedestrianMaxAreaDensityVoronoiProcessorId) {
        checkSealed();
        this.pedestrianAreaDensityVoronoiProcessorId = pedestrianMaxAreaDensityVoronoiProcessorId;
    }
}
