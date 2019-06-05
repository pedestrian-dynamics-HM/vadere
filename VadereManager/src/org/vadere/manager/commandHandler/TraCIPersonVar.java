package org.vadere.manager.commandHandler;

import org.vadere.manager.stsc.TraCIDataType;

public enum TraCIPersonVar {

	ID_LIST(0x00, TraCIDataType.STRING_LIST), // get
	COUNT(0x01, TraCIDataType.INTEGER), // get
	SPEED(0x40, TraCIDataType.DOUBLE), // get, set
	POS_2D(0x42, TraCIDataType.POS_2D), // get
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

	UNKNOWN(-1, TraCIDataType.UNKNOWN),
	;


	int id;
	TraCIDataType returnType;

	TraCIPersonVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.returnType = retVal;
	}

	public boolean isUnknown(){
		return this.id == UNKNOWN.id;
	}

	public static TraCIPersonVar fromId(int id){
		for(TraCIPersonVar var : values()){
			if (var.id == id)
				return var;
		}
		return UNKNOWN;
	}

	@Override
	public String toString() {
		return "TraCIPersonVar{" +
				"id=" + id +
				", returnType=" + returnType +
				'}';
	}
}
