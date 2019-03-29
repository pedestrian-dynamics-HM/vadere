package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesAreaDensityVoronoiProcessor extends AttributesAreaProcessor {
    private int voronoiMeasurementAreaIdArea = -1;

    public int getVoronoiMeasurementAreaIdArea() {
        return this.voronoiMeasurementAreaIdArea;
    }

    public void setVoronoiMeasurementAreaIdArea(int voronoiMeasurementAreaIdArea) {
        checkSealed();
        this.voronoiMeasurementAreaIdArea = voronoiMeasurementAreaIdArea;
    }
}
