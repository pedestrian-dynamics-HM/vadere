package org.vadere.manager.traci.compound.object;

import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.traci.CompoundObject;
import org.vadere.state.traci.CompoundObjectBuilder;
import org.vadere.util.geometry.shapes.VPoint;

public class CoordRef {

	private String epgsCode;
	private VPoint offset;

	public CoordRef(CompoundObject obj){
		if (obj.size() != 3){
			throw new TraCIException("Expected 4 elements for PointConverter");
		}
		epgsCode = (String) obj.getData(0, TraCIDataType.STRING);
		VPoint p = new VPoint();
		p.x = (Double) obj.getData(1, TraCIDataType.DOUBLE);
		p.y = (Double) obj.getData(2, TraCIDataType.DOUBLE);
		offset = p;
	}

	public CoordRef(String epgsCode, VPoint offset) {
		this.epgsCode = epgsCode;
		this.offset = offset;
	}

	public static CompoundObject asCompoundObject(String epgsCode,
												  VPoint offset){
		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING)
				.add(TraCIDataType.DOUBLE, 2)
				.build(epgsCode, offset.x, offset.y);
	}
}
