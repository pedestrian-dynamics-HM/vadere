package org.vadere.manager.commandHandler;

import org.vadere.manager.stsc.TraCIDataType;

public enum TraCIVariable {

	ID_LIST(0x00, TraCIDataType.STRING_LIST),
	COUNT(0x01, TraCIDataType.INTEGER),
	SPEED(0x40, TraCIDataType.DOUBLE),
	POS_2D(0x42, TraCIDataType.POS_2D),
	POS_3D(0x42, TraCIDataType.POS_3D),
	ANGLE(0x43, TraCIDataType.DOUBLE),
	SLOPE(0x36, TraCIDataType.DOUBLE),
	ROAD_ID(0x50, TraCIDataType.STRING),
	TYPE(0x4f, TraCIDataType.STRING),
	COLOR(0x45, TraCIDataType.COLOR),
	EDGE_POS(0x56, TraCIDataType.DOUBLE),
	LENGTH(0x44,TraCIDataType.DOUBLE),
	MIN_GAP(0x4c, TraCIDataType.DOUBLE),
	WIDTH(0x4d, TraCIDataType.DOUBLE),
	WAITING_TIME(0x7a,TraCIDataType.DOUBLE),
	NEXT_EDGE(0xc1, TraCIDataType.STRING),
	REMAINING_STAGES(0xc2, TraCIDataType.INTEGER),
	VEHICLE(0xc3, TraCIDataType.STRING),
	UNKNOWN(-1, TraCIDataType.UNKNOWN),
	;


	int id;
	TraCIDataType returnType;

	TraCIVariable(int id, TraCIDataType retVal) {
		this.id = id;
		this.returnType = retVal;
	}

	public boolean isUnknown(){
		return this.id == UNKNOWN.id;
	}


	@Override
	public String toString() {
		return "TraCIVariable{" +
				"id=" + id +
				", returnType=" + returnType +
				'}';
	}
}
