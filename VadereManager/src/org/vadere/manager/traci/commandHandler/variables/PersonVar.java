package org.vadere.manager.traci.commandHandler.variables;

import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIDataType;


/**
 * VariableId list for Person API.
 */
public enum PersonVar {

	ID_LIST(0x00, TraCIDataType.STRING_LIST), // get
	COUNT(0x01, TraCIDataType.INTEGER), // get
	NEXT_ID(0x02, TraCIDataType.INTEGER), // get
	HAS_NEXT_TARGET(0x03, TraCIDataType.INTEGER), // get
	NEXT_TARGET_LIST_INDEX(0x04, TraCIDataType.INTEGER), // get, set
	SPEED(0x40, TraCIDataType.DOUBLE), // get, set
	MAXSPEED(0x45, TraCIDataType.DOUBLE), // get
	VELOCITY(0x41, TraCIDataType.POS_2D), // get
	POSITION(0x42, TraCIDataType.POS_2D), // get, set
	POSITION_LIST(0xff, TraCIDataType.POS_2D_LIST), // get
	POSITION3D(0x42, TraCIDataType.POS_3D), // get
	ANGLE(0x43, TraCIDataType.DOUBLE), // get
	SLOPE(0x36, TraCIDataType.DOUBLE), // get
	ROAD_ID(0x50, TraCIDataType.STRING), // get
	TYPE(0x4f, TraCIDataType.STRING), // get, set
	COLOR(0x45, TraCIDataType.COLOR), // get, set
	EDGE_POS(0x56, TraCIDataType.DOUBLE), // get
	LENGTH(0x44, TraCIDataType.DOUBLE), // get, set
	MIN_GAP(0x4c, TraCIDataType.DOUBLE), // get, set
	WIDTH(0x4d, TraCIDataType.DOUBLE), // get, set
	WAITING_TIME(0x7a, TraCIDataType.DOUBLE), // get
	NEXT_EDGE(0xc1, TraCIDataType.STRING), // get
	REMAINING_STAGES(0xc2, TraCIDataType.INTEGER), // get
	VEHICLE(0xc3, TraCIDataType.STRING), // get
	ADD(0x80, TraCIDataType.STRING),
	REMOVE_STAGE(0xc5, TraCIDataType.INTEGER), // set
	TARGET_LIST(0xfe, TraCIDataType.STRING_LIST), // get, set            
	INFORMATION_ITEM(0xfd, TraCIDataType.COMPOUND_OBJECT)
	;


	public int id;
	public TraCIDataType type;

	PersonVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.type = retVal;
	}


	public static PersonVar fromId(int id) {
		for (PersonVar var : values()) {
			if (var.id == id)
				return var;
		}
		throw new TraCIException(String.format("No person var found with id: %02X", id));
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

	public String logShort() {
		return String.format("{%s:0x%02X}", name(), id);
	}
}
