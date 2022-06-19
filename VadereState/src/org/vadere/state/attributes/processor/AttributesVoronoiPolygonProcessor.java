package org.vadere.state.attributes.processor;

public class AttributesVoronoiPolygonProcessor extends AttributesProcessor  {
	private int voronoiMeasurementAreaId;

	public int getVoronoiMeasurementAreaId() {
		return voronoiMeasurementAreaId;
	}

	public void setVoroniMeasurementAreaIdArea(int voronoiMeasurementAreaId) {
		checkSealed();
		this.voronoiMeasurementAreaId = voronoiMeasurementAreaId;
	}
}
