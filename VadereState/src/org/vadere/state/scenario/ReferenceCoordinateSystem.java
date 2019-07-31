package org.vadere.state.scenario;

import org.vadere.util.geometry.shapes.VPoint;

import java.util.Objects;

public class ReferenceCoordinateSystem {

	/**
	 * international identifier for the base coordinate reference system (CRS) which was used
	 * to build this topography.
	 * e.g.
	 *
	 * EPSG:4326 = WGS-84 (lon/lat from GPS systems, used in OpenStreetMap)
	 * EPSG:25832 = ETRS89 / UTM Zone 32N (UTM Zone System) zone 32 contains most of germany.
	 *
	 * see
	 * https://de.wikipedia.org/wiki/European_Petroleum_Survey_Group_Geodesy#EPSG-Codes
	 * https://de.wikipedia.org/wiki/UTM-Koordinatensystem#Aufbau
	 * for references
	 */
	private String epsgCode = "";
	/**
	 * short description of epsgCode. e.g. "WGS-84 OpenStreetMap (OSM)" for none GIS nerds ;)
	 */
	private String description = "";

	/**
	 * Translation vector used to move topography to origin (0,0)
	 */
	private VPoint translation = new VPoint(0.0,0.0);

	public ReferenceCoordinateSystem() {
	}

	public ReferenceCoordinateSystem(String epsgCode, String description, VPoint translation) {
		this.epsgCode = epsgCode;
		this.description = description;
		this.translation = translation;
	}

	public String getEpsgCode() {
		return epsgCode;
	}

	public void setEpsgCode(String epsgCode) {
		this.epsgCode = epsgCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public VPoint getTranslation() {
		return translation;
	}

	public void setTranslation(VPoint translation) {
		this.translation = translation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ReferenceCoordinateSystem that = (ReferenceCoordinateSystem) o;
		return Objects.equals(epsgCode, that.epsgCode) &&
				Objects.equals(description, that.description) &&
				Objects.equals(translation, that.translation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(epsgCode, description, translation);
	}

	@Override
	public String toString() {
		return "ReferenceCoordinateSystem{" +
				"epsgCode='" + epsgCode + '\'' +
				", description='" + description + '\'' +
				", translation=" + translation +
				'}';
	}
}
