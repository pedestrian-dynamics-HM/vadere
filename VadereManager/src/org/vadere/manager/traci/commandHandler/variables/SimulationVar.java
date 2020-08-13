package org.vadere.manager.traci.commandHandler.variables;

import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.manager.traci.commands.TraCICommand;

public enum SimulationVar {

	CURR_SIM_TIME(0x66, TraCIDataType.DOUBLE),
	VAR_DELTA_T(0x7b, TraCIDataType.DOUBLE), //step length
	NUM_LOADED_VEHICLES(0x71, TraCIDataType.INTEGER),
	LOADED_VEHICLES_IDS(0x72, TraCIDataType.STRING_LIST),
	NUM_DEPARTED_PEDESTRIAN(0x73, TraCIDataType.INTEGER), // alias NUM_DEPARTED_VEHICLES
	DEPARTED_PEDESTRIAN_IDS(0x74, TraCIDataType.STRING_LIST), // alias DEPARTED_VEHICLES_IDS
	NUM_ARRIVED_PEDESTRIAN(0x79, TraCIDataType.INTEGER), // alias VAR_ARRIVED_VEHICLES_NUMBER
	ARRIVED_PEDESTRIAN_PEDESTRIAN_IDS(0x7a, TraCIDataType.STRING_LIST), // alias VAR_ARRIVED_VEHICLES_IDS
	POSITION_CONVERSION(0x82, TraCIDataType.COMPOUND_OBJECT),
	NUM_VEHICLES_START_TELEPORT(0x75, TraCIDataType.INTEGER),
	VEHICLES_START_TELEPORT_IDS(0x76, TraCIDataType.STRING_LIST),
	NUM_VEHICLES_END_TELEPORT(0x77, TraCIDataType.INTEGER),
	VEHICLES_END_TELEPORT_IDS(0x78, TraCIDataType.STRING_LIST),
	VEHICLES_START_PARKING_IDS(0x6d, TraCIDataType.STRING_LIST),
	VEHICLES_STOP_PARKING_IDS(0x6f, TraCIDataType.STRING_LIST),
	//
	NETWORK_BOUNDING_BOX_2D(0x7c, TraCIDataType.POLYGON),
	CACHE_HASH(0x7d, TraCIDataType.STRING),
	SIM_CONFIG(0x7e, TraCIDataType.COMPOUND_OBJECT),
	;

	public int id;
	public TraCIDataType type;

	SimulationVar(int id, TraCIDataType retVal) {
		this.id = id;
		this.type = retVal;
	}

	public static SimulationVar fromId(int id) {
		for (SimulationVar var : values()) {
			if (var.id == id)
				return var;
		}
		throw new TraCIException(String.format("No simulation var found with id: %02X", id));
	}

	@Override
	public String toString() {
		return "SimulationVar{" +
				"id=" + id +
				", type=" + type +
				'}';
	}
}
