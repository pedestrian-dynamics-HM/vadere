package org.vadere.manager.stsc;

public enum TraCIDataType {
	U_BYTE(0x07, 1, true),
	BYTE(0x08, 1, true),
	INTEGER(0x09,4,true),
	DOUBLE(0x0B,8,true),
	STRING(0x0C,-1,true),
	STRING_LIST(0x0E,-1,true),
	COMPOUND_OBJECT(0x0F,-1,true),
	POS_2D(0x01,17,false),
	POS_3D(0x03,25,false),
	POS_ROAD_MAP(0x04, -1,false),
	POS_LON_LAT(0x00,17,false),
	POS_LON_LAT_ALT(0x02, 25,false),
	POLYGON(0x06,-1,false),
	TRAFFIC_LIGHT_PHASE_LIST(0x0D,-1,false),
	COLOR(0x11,5,false),
	UNKNOWN(-1, -1, false)
	;



	public int identifier;
	public int size_in_byte;
	public boolean isAtomar;


	TraCIDataType(int identifier, int size_in_byte, boolean isAtomar){
		this.identifier = identifier;
		this.size_in_byte = size_in_byte;
		this.isAtomar = isAtomar;
	}

	public boolean isUnknown(){
		return this.identifier == UNKNOWN.identifier;
	}

	public static TraCIDataType fromId(int id){
		for(TraCIDataType dataType : values()){
			if (dataType.identifier == id)
				return dataType;
		}
		return UNKNOWN;
	}

	@Override
	public String toString() {
		return "TraCIDataType{" +
				"identifier=" + identifier +
				", size_in_byte=" + size_in_byte +
				", isAtomar=" + isAtomar +
				'}';
	}
}
