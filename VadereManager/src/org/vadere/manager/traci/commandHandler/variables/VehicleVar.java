package org.vadere.manager.traci.commandHandler.variables;

import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIDataType;

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
				", type=" + returnType +
				'}';
	}
}
