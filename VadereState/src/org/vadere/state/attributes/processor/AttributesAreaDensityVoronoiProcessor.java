package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesAreaDensityVoronoiProcessor extends AttributesAreaProcessor {
    private int voronoiMeasurementAreaId = -1;

    public int getVoronoiMeasurementAreaId() {
        return this.voronoiMeasurementAreaId;
    }

    public void setVoronoiMeasurementAreaId(int voronoiMeasurementAreaId) {
        checkSealed();
        this.voronoiMeasurementAreaId = voronoiMeasurementAreaId;
    }
}
