package org.vadere.manager.traci.commandHandler.variables;

import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;

public enum VadereVar {
	ADD_TARGET_CHANGER(0x00, TraCIDataType.STRING),
	REMOVE_TARGET_CHANGER(0x01, TraCIDataType.NULL),
	ADD_STIMULUS_INFOS(0x02, TraCIDataType.STRING),
	GET_ALL_STIMULUS_INFOS(0x03, TraCIDataType.STRING),
	;

	public int id;
	public TraCIDataType type;

	VadereVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.type = retVal;
	}


	public static VadereVar fromId(int id) {
		for (VadereVar var : values()) {
			if (var.id == id)
				return var;
		}
		throw new TraCIException(String.format("No vadere var found with id: %02X", id));
	}
}
