package org.vadere.manager.traci.commandHandler.variables;

import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;

public enum ControlVar {
	NONE(-1, TraCIDataType.INTEGER);

	public int id;
	public TraCIDataType returnType;

	ControlVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.returnType = retVal;
	}


	public static ControlVar fromId(int id) {
		for (ControlVar var : values()) {
			if (var.id == id)
				return var;
		}
		throw new TraCIException(String.format("No ControlVar var found with id: %02X", id));
	}

	@Override
	public String toString() {
		return "ControlVar{" +
				"id=" + id +
				", type=" + returnType +
				'}';
	}
}
