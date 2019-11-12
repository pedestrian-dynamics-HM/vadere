package org.vadere.manager.traci.commandHandler.variables;

import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;


/**
 * VariableId list for Person API.
 */
public enum PersonVar {

	ID_LIST(0x00, TraCIDataType.STRING_LIST), // get
	COUNT(0x01, TraCIDataType.INTEGER), // get
	NEXT_ID(0x02, TraCIDataType.INTEGER), // get
	SPEED(0x40, TraCIDataType.DOUBLE), // get, set
	POS_2D(0x42, TraCIDataType.POS_2D), // get, set
	POS_2D_LIST(0xff, TraCIDataType.POS_2D_LIST), // get
	POS_3D(0x42, TraCIDataType.POS_3D), // get
	ANGLE(0x43, TraCIDataType.DOUBLE), // get
	SLOPE(0x36, TraCIDataType.DOUBLE), // get
	ROAD_ID(0x50, TraCIDataType.STRING), // get
	TYPE(0x4f, TraCIDataType.STRING), // get, set
	COLOR(0x45, TraCIDataType.COLOR), // get, set
	EDGE_POS(0x56, TraCIDataType.DOUBLE), // get
	LENGTH(0x44,TraCIDataType.DOUBLE), // get, set
	MIN_GAP(0x4c, TraCIDataType.DOUBLE), // get, set
	WIDTH(0x4d, TraCIDataType.DOUBLE), // get, set
	WAITING_TIME(0x7a,TraCIDataType.DOUBLE), // get
	NEXT_EDGE(0xc1, TraCIDataType.STRING), // get
	REMAINING_STAGES(0xc2, TraCIDataType.INTEGER), // get
	VEHICLE(0xc3, TraCIDataType.STRING), // get
	ADD(0x80, TraCIDataType.COMPOUND_OBJECT), // set
	APPEND_STAGE(0xc4, TraCIDataType.COMPOUND_OBJECT), // set
	REMOVE_STAGE(0xc5, TraCIDataType.INTEGER), // set
	REROUTE(0x90, TraCIDataType.COMPOUND_OBJECT), // set
	TARGET_LIST(0xfe, TraCIDataType.STRING_LIST), // get, set
	HEURISTIC(0xd0, TraCIDataType.STRING), // get, set
	;


	public int id;
	public TraCIDataType type;

	PersonVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.type = retVal;
	}


	public static PersonVar fromId(int id){
		for(PersonVar var : values()){
			if (var.id == id)
				return var;
		}
		throw new TraCIException(String.format("No person var found with id: %02X", id));
	}

	@Override
	public String toString() {
		return "PersonVar{" +
				name() +
				": id=" + id +
				", type=" + type +
				'}';
	}
}
