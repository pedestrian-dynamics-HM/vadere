package org.vadere.manager.traci.commandHandler;

import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;

public enum TraCIVehicleVar {
	ID_LIST(0x00, TraCIDataType.STRING_LIST);

	public int id;
	public TraCIDataType returnType;

	TraCIVehicleVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.returnType = retVal;
	}


	public static TraCIVehicleVar fromId(int id){
		for(TraCIVehicleVar var : values()){
			if (var.id == id)
				return var;
		}
		throw new TraCIException(String.format("No vehicle variable found with id: %02X", id));
	}

	@Override
	public String toString() {
		return "TraCIVehicleVar{" +
				"id=" + id +
				", returnType=" + returnType +
				'}';
	}
}
