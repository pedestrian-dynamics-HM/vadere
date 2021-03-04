package org.vadere.manager.traci.commandHandler.variables;

import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIDataType;

public enum PolygonVar {
	ID_LIST(0x00, TraCIDataType.STRING_LIST), // get
	COUNT(0x01, TraCIDataType.INTEGER), // get
	TOPOGRAPHY_BOUNDS(0x02, TraCIDataType.STRING_LIST), // get
	SHAPE(0x4e, TraCIDataType.POLYGON), // get
	TYPE(0x4f, TraCIDataType.STRING), // get, set
	CENTROID(0x50, TraCIDataType.POS_2D), // get
	DISTANCE(0x51, TraCIDataType.STRING_LIST), // get
	COLOR(0x45, TraCIDataType.COLOR), // get, set
	POSITION(0x42, TraCIDataType.POS_2D), // get
	IMAGEFILE(0x93, TraCIDataType.STRING),
	WIDTH(0x4d, TraCIDataType.DOUBLE), // get, set
	HEIGHT(0xbc, TraCIDataType.DOUBLE), // get, set
	ANGLE(0x43, TraCIDataType.DOUBLE);

	public int id;
	public TraCIDataType type;

	PolygonVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.type = retVal;
	}


	public static PolygonVar fromId(int id) {
		for (PolygonVar var : values()) {
			if (var.id == id)
				return var;
		}
		throw new TraCIException(String.format("No polygon var found with id: %02X", id));
	}

	public static String asString(int id){
		try {
			return fromId(id).toString();
		} catch (TraCIException ignored) { }
		return "No variable for id: " + id;
	}

	@Override
	public String toString() {
		return name() + "{" +
				"id=" + id +
				", type=" + type +
				'}';
	}
}
