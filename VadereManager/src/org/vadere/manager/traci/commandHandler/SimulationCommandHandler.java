package org.vadere.manager.traci.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.respons.TraCIGetResponse;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * Handel GET/SET/SUB {@link org.vadere.manager.traci.commands.TraCICommand}s for the Simulation API
 */
public class SimulationCommandHandler  extends CommandHandler{

	public static SimulationCommandHandler instance;

	static {
		instance = new SimulationCommandHandler();
	}

	private SimulationCommandHandler(){}

	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData){
		return responseOK(responseDataType, responseData, TraCICmd.GET_SIMULATION_VALUE, TraCICmd.RESPONSE_GET_SIMULATION_VALUE);
	}

	public TraCIGetResponse responseERR(TraCIDataType responseDataType, Object responseData){
		return responseOK(responseDataType, responseData, TraCICmd.GET_SIMULATION_VALUE, TraCICmd.RESPONSE_GET_SIMULATION_VALUE);
	}

	private TraCICommand process_getNetworkBound(TraCIGetCommand cmd, RemoteManager remoteManager, TraCISimulationVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			Rectangle2D.Double  rec = state.getTopography().getBounds();

			VPoint lowLeft = new VPoint(rec.getMinX(), rec.getMinY());
			VPoint highRight = new VPoint(rec.getMaxX(), rec.getMaxX());
			ArrayList<VPoint> polyList = new ArrayList<>();
			polyList.add(lowLeft);
			polyList.add(highRight);
			cmd.setResponse(responseOK(traCIVar.returnType, polyList));
		});

		return cmd;
	}

	private TraCICommand process_getSimTime(TraCIGetCommand cmd, RemoteManager remoteManager, TraCISimulationVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			// BigDecimal to ensure correct comparison in omentpp
			BigDecimal time = BigDecimal.valueOf(state.getSimTimeInSec());
			cmd.setResponse(responseOK(traCIVar.returnType, time.setScale(1, RoundingMode.HALF_UP).doubleValue()));
		});

		return cmd;
	}

	private  TraCICommand process_getVehiclesStartTeleportIDs(TraCIGetCommand cmd, RemoteManager remoteManager, TraCISimulationVar traCIVar){

		cmd.setResponse(responseOK(traCIVar.returnType, new ArrayList<>()));
		return cmd;
	}

	private  TraCICommand process_getVehiclesEndTeleportIDs(TraCIGetCommand cmd, RemoteManager remoteManager, TraCISimulationVar traCIVar){

		cmd.setResponse(responseOK(traCIVar.returnType, new ArrayList<>()));
		return cmd;
	}

	private  TraCICommand process_getVehiclesStartParkingIDs(TraCIGetCommand cmd, RemoteManager remoteManager, TraCISimulationVar traCIVar){

		cmd.setResponse(responseOK(traCIVar.returnType, new ArrayList<>()));
		return cmd;
	}

	private  TraCICommand process_getVehiclesStopParkingIDs(TraCIGetCommand cmd, RemoteManager remoteManager, TraCISimulationVar traCIVar){

		cmd.setResponse(responseOK(traCIVar.returnType, new ArrayList<>()));
		return cmd;
	}

	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager){
		return processValueSub(rawCmd, remoteManager, this::processGet,
				TraCICmd.SUB_SIMULATION_VALUE, TraCICmd.RESPONSE_SUB_SIMULATION_VALUE);
	}


	public TraCICommand processGet(TraCICommand rawCmd, RemoteManager remoteManager){

		TraCIGetCommand cmd = (TraCIGetCommand) rawCmd;
		TraCISimulationVar var = TraCISimulationVar.fromId(cmd.getVariableIdentifier());
		switch (var){
			case NETWORK_BOUNDING_BOX_2D:
				return process_getNetworkBound(cmd, remoteManager, var);
			case CURR_SIM_TIME:
				return process_getSimTime(cmd, remoteManager, var);
			case VEHICLES_START_TELEPORT_IDS:
				return process_getVehiclesStartTeleportIDs(cmd, remoteManager, var);
			case VEHICLES_END_TELEPORT_IDS:
				return process_getVehiclesEndTeleportIDs(cmd, remoteManager, var);
			case VEHICLES_START_PARKING_IDS:
				return process_getVehiclesStartParkingIDs(cmd, remoteManager, var);
			case VEHICLES_STOP_PARKING_IDS:
				return process_getVehiclesStopParkingIDs(cmd, remoteManager, var);

			default:
				return process_NotImplemented(cmd, remoteManager);
		}
	}

	public TraCICommand processSet(TraCICommand cmd, RemoteManager remoteManager) {
		return process_NotImplemented(cmd, remoteManager);

	}

}
