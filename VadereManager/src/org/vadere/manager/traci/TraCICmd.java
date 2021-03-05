package org.vadere.manager.traci;

import org.vadere.state.traci.TraCIException;
import org.vadere.manager.traci.commandHandler.variables.ControlVar;
import org.vadere.manager.traci.commandHandler.variables.PersonVar;
import org.vadere.manager.traci.commandHandler.variables.PolygonVar;
import org.vadere.manager.traci.commandHandler.variables.SimulationVar;
import org.vadere.manager.traci.commandHandler.variables.VadereVar;

/**
 * List of all TraCI Commands and there Types.
 */
public enum TraCICmd {
	// TraCI/Control-related commands
	GET_VERSION(0x00, CmdType.CTRL, ControlVar::asString),
	SIM_STEP(0x02, CmdType.CTRL, ControlVar::asString),
	CLOSE(0x7F, CmdType.CTRL, ControlVar::asString),
	LOAD(0x01, CmdType.CTRL, ControlVar::asString),
	SET_ORDER(0x03, CmdType.CTRL, ControlVar::asString),
	GET_STATE(0x04, CmdType.CTRL, ControlVar::asString),
	SEND_FILE(0x75, CmdType.CTRL, ControlVar::asString),
	// Value Retrieval
	GET_INDUCTION_LOOP(0xa0, CmdType.VALUE_GET),
	RESPONSE_GET_INDUCTION_LOOP(0xb0, CmdType.RESPONSE),
	GET_MULTI_ENTRY_EXIT_DETECTOR(0xa1, CmdType.VALUE_GET),
	RESPONSE_GET_MULTI_ENTRY_EXIT_DETECTOR(0xb1, CmdType.RESPONSE),
	GET_TRAFFIC_LIGHT_VALUE(0xa2, CmdType.VALUE_GET),
	RESPONSE_GET_TRAFFIC_LIGHT_VALUE(0xb2, CmdType.RESPONSE),
	GET_LANE_VALUE(0xa3, CmdType.VALUE_GET),
	RESPONSE_GET_LANE_VALUE(0xb3, CmdType.RESPONSE),
	GET_VEHICLE_VALUE(0xa4, CmdType.VALUE_GET),
	RESPONSE_GET_VEHICLE_VALUE(0xb4, CmdType.RESPONSE),
	GET_VEHICLE_TYPE_VALUE(0xa5, CmdType.VALUE_GET),
	RESPONSE_GET_VEHICLE_TYPE_VALUE(0xb5, CmdType.RESPONSE),
	GET_ROUTE_VALUE(0xa6, CmdType.VALUE_GET),
	RESPONSE_GET_ROUTE_VALUE(0xb6, CmdType.RESPONSE),
	GET_POI_VALUE(0xa7, CmdType.VALUE_GET),
	RESPONSE_GET_POI_VALUE(0xb7, CmdType.RESPONSE),

	GET_POLYGON(0xa8, CmdType.VALUE_GET, PolygonVar::asString),
	RESPONSE_GET_POLYGON(0xb8, CmdType.RESPONSE, PolygonVar::asString),
	GET_JUNCTION_VALUE(0xa9, CmdType.VALUE_GET),
	RESPONSE_GET_JUNCTION_VALUE(0xb9, CmdType.RESPONSE),
	GET_EDGE_VALUE(0xaa, CmdType.VALUE_GET),
	RESPONSE_GET_EDGE_VALUE(0xba, CmdType.RESPONSE),
	GET_SIMULATION_VALUE(0xab, CmdType.VALUE_GET, SimulationVar::asString),
	RESPONSE_GET_SIMULATION_VALUE(0xbb, CmdType.RESPONSE, SimulationVar::asString),
	GET_GUI_VALUE(0xac, CmdType.VALUE_GET),
	RESPONSE_GET_GUI_VALUE(0xbc, CmdType.RESPONSE),
	GET_LANEAREA_DETECTOR(0xad, CmdType.VALUE_GET),
	RESPONSE_GET_LANEAREA_DETECTOR(0xbd, CmdType.RESPONSE),
	GET_PERSON_VALUE(0xae, CmdType.VALUE_GET, PersonVar::asString),
	RESPONSE_GET_PERSON_VALUE(0xbe, CmdType.RESPONSE, PersonVar::asString),
	GET_VADERE_VALUE(0xaf, CmdType.VALUE_GET, VadereVar::asString),
	RESPONSE_GET_VADERE_VALUE(0xbf, CmdType.RESPONSE, VadereVar::asString),
	// State Changing
	SET_TRAFFIC_LIGHT_STATE(0xc2, CmdType.VALUE_SET),
	SET_LANE_STATE(0xc3, CmdType.VALUE_SET),
	SET_VEHICLE_STATE(0xc4, CmdType.VALUE_SET),
	SET_PERSON_STATE(0xce, CmdType.VALUE_SET),
	SET_VADERE_STATE(0xcf, CmdType.VALUE_SET),
	SET_VEHICLE_TYPE_STATE(0xc5, CmdType.VALUE_SET),
	SET_ROUTE_STATE(0xc6, CmdType.VALUE_SET),
	SET_POT_STATE(0xc7, CmdType.VALUE_SET),
	SET_POLYGON_STATE(0xc8, CmdType.VALUE_SET),
	SET_EDGE_STATE(0xca, CmdType.VALUE_SET),
	SET_SIMULATION_STATE(0xcb, CmdType.VALUE_SET), // changed: 0xcc -> 0xcb to match sumo/tools/traci
	SET_GUI_STATE(0xcc, CmdType.VALUE_SET),
	// TraCI/Object Variable Subscription
	SUB_INDUCTION_LOOP_VALUE(0xd0, CmdType.VALUE_SUB),
	RESPONSE_SUB_INDUCTION_LOOP_VALUE(0xe0, CmdType.RESPONSE),
	SUB_MULTI_ENTRY_EXIT_DETECTOR_VALUE(0xd1, CmdType.VALUE_SUB),
	RESPONSE_SUB_MULTI_ENTRY_EXIT_DETECTOR_VALUE(0xe1, CmdType.RESPONSE),
	SUB_TRAFFIC_LIGHT_VALUE(0xd2, CmdType.VALUE_SUB),
	RESPONSE_SUB_TRAFFIC_LIGHT_VALUE(0xe2, CmdType.RESPONSE),
	SUB_LANE_VALUE(0xd3, CmdType.VALUE_SUB),
	RESPONSE_SUB_LANE_VALUE(0xe3, CmdType.RESPONSE),
	SUB_VEHICLE_VALUE(0xd4, CmdType.VALUE_SUB),
	RESPONSE_SUB_VEHICLE_VALUE(0xe4, CmdType.RESPONSE),
	SUB_VEHICLE_TYPE_VALUE(0xd5, CmdType.VALUE_SUB),
	RESPONSE_SUB_VEHICLE_TYPE_VALUE(0xe5, CmdType.RESPONSE),
	SUB_ROUTE_VALUE(0xd6, CmdType.VALUE_SUB),
	RESPONSE_SUB_ROUTE_VALUE(0xe6, CmdType.RESPONSE),
	SUB_POI_VALUE(0xd7, CmdType.VALUE_SUB),
	RESPONSE_SUB_POI_VALUE(0xe7, CmdType.RESPONSE),
	SUB_POLYGON_VALUE(0xd8, CmdType.VALUE_SUB),
	RESPONSE_SUB_POLYGON_VALUE(0xe8, CmdType.RESPONSE),
	SUB_JUNCTION_VALUE(0xd9, CmdType.VALUE_SUB),
	RESPONSE_SUB_JUNCTION_VALUE(0xe9, CmdType.RESPONSE),
	SUB_EDGE_VALUE(0xda, CmdType.VALUE_SUB),
	RESPONSE_SUB_EDGE_VALUE(0xea, CmdType.RESPONSE),
	SUB_SIMULATION_VALUE(0xdb, CmdType.VALUE_SUB),
	RESPONSE_SUB_SIMULATION_VALUE(0xeb, CmdType.RESPONSE),
	SUB_PERSON_VARIABLE(0xde, CmdType.VALUE_SUB),
	RESPONSE_SUB_PERSON_VARIABLE(0xee, CmdType.RESPONSE),
	SUB_VADERE_VARIABLE(0xdf, CmdType.VALUE_SUB),
	RESPONSE_SUB_VADERE_VALUE(0xef, CmdType.RESPONSE),
	// TraCI/Object Context Subscription
	;

	public int id;
	public CmdType type;
	public  VarString varString;

	TraCICmd(int id, CmdType type) {
		this.id = id;
		this.type = type;
		this.varString = id1 -> "varID: " + id1;
	}

	TraCICmd(int id, CmdType type, VarString varString) {
		this.id = id;
		this.type = type;
		this.varString = varString;
	}


	public static TraCICmd fromId(int id) {
		for (TraCICmd traCICmd : values()) {
			if (traCICmd.id == id)
				return traCICmd;
		}
		throw new TraCIException(String.format("No TraCI command found with id: %02X", id));
	}

	@Override
	public String toString() {
		return String.format("TraCICmd{%s: id=0x%02X, type=%s}", name(), id, type);
	}

	public String logShort() {
		return String.format("{%s: 0x%02X %s}", name(), id, type);
	}

	public String logShort(int varId) {
		return String.format("{%s: 0x%02X %s VAR: %s}", name(), id, type, varString.varAsString(varId));
	}
}
