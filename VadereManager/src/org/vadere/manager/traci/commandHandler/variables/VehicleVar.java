package org.vadere.manager.traci.commandHandler.variables;

import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;

public enum VehicleVar {
	ID_LIST(0x00, TraCIDataType.STRING_LIST);

	public int id;
	public TraCIDataType returnType;

	VehicleVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.returnType = retVal;
	}


	public static VehicleVar fromId(int id) {
		for (VehicleVar var : values()) {
			if (var.id == id)
				return var;
		}
		throw new TraCIException(String.format("No vehicle var found with id: %02X", id));
	}

	@Override
	public String toString() {
		return "VehicleVar{" +
				"id=" + id +
				", type=" + returnType +
				'}';
	}
}
