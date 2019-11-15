package org.vadere.manager.traci.compoundobjects;

import org.vadere.manager.traci.TraCIDataType;
import org.vadere.util.geometry.shapes.VPoint;

public class IdPosData extends GenericCompoundObject {

	String id;
	VPoint pos;

	IdPosData(CompoundObject o) {
		super(o, 2);
	}

	@Override
	protected void init(CompoundObject o) {
		id = (String)o.getData(0, TraCIDataType.STRING);
		pos = (VPoint)o.getData(1, TraCIDataType.POS_2D);
	}

	public String getId() {
		return id;
	}

	public VPoint getPos() {
		return pos;
	}
}
