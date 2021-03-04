package org.vadere.manager.traci.compound.object;

import org.apache.commons.math3.util.Pair;
import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.traci.CompoundObject;
import org.vadere.state.scenario.ReferenceCoordinateSystem;
import org.vadere.util.geometry.shapes.VPoint;

public class PointConverter {

	enum PointType {
		POSITION_LON_LAT,
		POSITION_2D,
	}

	private VPoint p; // (lon, lat) Order
	private PointType conversionType;


	public PointConverter(CompoundObject obj){
		if (obj.size() != 2)
			throw new TraCIException("Expected 3 elements for PointConverter");
		if (obj.hasIndex(0, TraCIDataType.POS_2D)){
			p = (VPoint) obj.getData(0, TraCIDataType.POS_2D);
		} else if (obj.hasIndex(0, TraCIDataType.POS_LON_LAT)){
			p = (VPoint) obj.getData(0, TraCIDataType.POS_LON_LAT);
		} else {
			throw new TraCIException("Unknown PointType");
		}
		int type = (Integer) obj.getData(1, TraCIDataType.U_BYTE);
		if (type == 0x00){
			conversionType = PointType.POSITION_LON_LAT;
		} else if (type == 0x01){
			conversionType = PointType.POSITION_2D;
		} else {
			throw new TraCIException("Unknown ConversionType");
		}
	}

	public Pair<TraCIDataType, VPoint> convert(ReferenceCoordinateSystem coord){
		Pair<TraCIDataType, VPoint>ret;
		switch (conversionType){
			case POSITION_2D:
				ret = Pair.create(TraCIDataType.POS_2D, coord.convertToCartesian(p.y, p.x)); // LAT LONG axis order!!!
				break;
			case POSITION_LON_LAT:
				ret = Pair.create(TraCIDataType.POS_LON_LAT, coord.convertToGeo(new VPoint(p.x, p.y)));
				break;
			default:
				throw new TraCIException("Unkown ConversionType");
		}
		return ret;
	}

	public VPoint getP() {
		return p;
	}

	public void setP(VPoint p) {
		this.p = p;
	}

	public PointType getConversionType() {
		return conversionType;
	}

	public void setConversionType(PointType conversionType) {
		this.conversionType = conversionType;
	}
}
