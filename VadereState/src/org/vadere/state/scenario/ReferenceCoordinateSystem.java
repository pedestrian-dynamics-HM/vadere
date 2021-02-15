package org.vadere.state.scenario;

import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.Objects;

public class ReferenceCoordinateSystem {

	private static Logger logger = Logger.getLogger(ReferenceCoordinateSystem.class);

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

	transient private CoordinateReferenceSystem geoCRS;
	transient private CoordinateReferenceSystem utmCRS;
	transient private CoordinateOperation geoToUtm;
	transient private CoordinateOperation utmToGeo;
	transient private boolean initialized;

	/**
	 * Translation vector used to move topography to origin (0,0)
	 */
	private VPoint translation = new VPoint(0.0,0.0);

	public ReferenceCoordinateSystem() {
		this.initialized = false;
	}

	public ReferenceCoordinateSystem(String epsgCode, String description, VPoint translation) {
		this.epsgCode = epsgCode;
		this.description = description;
		this.translation = translation;
		this.initialized = false;
	}

	public void initialize(){
		if (initialized)
			return;

		try {
			utmCRS = CRS.forCode(epsgCode);
			geoCRS = ((DefaultProjectedCRS) utmCRS).getBaseCRS();
			geoToUtm = CRS.findOperation(geoCRS, utmCRS, null);
			utmToGeo = CRS.findOperation(utmCRS, geoCRS, null);
		} catch (FactoryException e) {
			logger.errorf(e.getMessage());
			throw new RuntimeException(e);
		}

		initialized = true;
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

	public boolean supportsConversion(){
		return geoToUtm != null && utmToGeo != null;
	}

	public VPoint convertToGeo(VPoint cartesian){
		if (!initialized)
			initialize();

		VPoint translated  = cartesian.add(translation);
		DirectPosition ptSrc = new DirectPosition2D(translated.x, translated.y);
		try {
			DirectPosition ptDst = utmToGeo.getMathTransform().transform(ptSrc, null);
			return new VPoint(ptDst.getCoordinate()[0], ptDst.getCoordinate()[1]);
		} catch (TransformException e) {
			logger.errorf(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public VPoint convertToCartesian(double latitude, double longitude ){
		if (!initialized)
			initialize();
		//  geographic CRS, the (latitude, longitude) axis order!
		DirectPosition ptSrc = new DirectPosition2D(latitude, longitude);
		try {
			DirectPosition ptDst = geoToUtm.getMathTransform().transform(ptSrc, null);
			VPoint ret =  new VPoint(ptDst.getCoordinate()[0], ptDst.getCoordinate()[1]);
			return ret.subtract(this.translation);
		} catch (TransformException e) {
			logger.errorf(e.getMessage());
			throw new RuntimeException(e);
		}
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
