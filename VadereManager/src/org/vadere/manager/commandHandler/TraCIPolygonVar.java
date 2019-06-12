package org.vadere.manager.commandHandler;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.TraCIDataType;

public enum TraCIPolygonVar {
	ID_LIST(0x00, TraCIDataType.STRING_LIST), // get
	COUNT(0x01, TraCIDataType.INTEGER), // get
	TYPE(0x4f, TraCIDataType.STRING), // get, set
	SHAPE(0x4e, TraCIDataType.POLYGON),
	COLOR(0x45, TraCIDataType.COLOR), // get, set
	POS_2D(0x42, TraCIDataType.POS_2D), // get
	IMAGE_FILE(0x93, TraCIDataType.STRING),
	WIDTH(0x4d, TraCIDataType.DOUBLE), // get, set
	HEIGHT(0xbc, TraCIDataType.DOUBLE), // get, set
	ANGLE(0x43, TraCIDataType.DOUBLE)
	;

	public int id;
	public TraCIDataType returnType;

	TraCIPolygonVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.returnType = retVal;
	}


	public static TraCIPolygonVar fromId(int id){
		for(TraCIPolygonVar var : values()){
			if (var.id == id)
				return var;
		}
		throw new TraCIException(String.format("No polygon variable found with id: %02X", id));
	}
}
