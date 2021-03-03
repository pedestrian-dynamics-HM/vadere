package org.vadere.manager.traci.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCISetCommand;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.util.logging.Logger;

import java.util.HashMap;

/**
 * Dispatcher for {@link TraCICommand}s.
 */
public class CommandExecutor {

	private static Logger logger = Logger.getLogger(CommandExecutor.class);

	private HashMap<Integer, TraCICmdHandler> cmdMap;
	private RemoteManager remoteManager;

	public CommandExecutor(RemoteManager remoteManager) {
		this.remoteManager = remoteManager;
		cmdMap = new HashMap<>();
		cmdMap.put(TraCICmd.GET_VERSION.id, ControlCommandHandler.instance::process_getVersion);
		cmdMap.put(TraCICmd.LOAD.id, ControlCommandHandler.instance::process_load);
		cmdMap.put(TraCICmd.SIM_STEP.id, ControlCommandHandler.instance::process_simStep);
		cmdMap.put(TraCICmd.GET_STATE.id, ControlCommandHandler.instance::process_getState);
		cmdMap.put(TraCICmd.CLOSE.id, ControlCommandHandler.instance::process_close);
		cmdMap.put(TraCICmd.SEND_FILE.id, ControlCommandHandler.instance::process_load_file);
		cmdMap.put(TraCICmd.GET_PERSON_VALUE.id, PersonCommandHandler.instance::processGet);
		cmdMap.put(TraCICmd.SET_PERSON_STATE.id, PersonCommandHandler.instance::processSet);
		cmdMap.put(TraCICmd.SUB_PERSON_VARIABLE.id, PersonCommandHandler.instance::processValueSub);
		cmdMap.put(TraCICmd.GET_VADERE_VALUE.id, VadereCommandHandler.instance::processGet);
		cmdMap.put(TraCICmd.SET_VADERE_STATE.id, VadereCommandHandler.instance::processSet);
		cmdMap.put(TraCICmd.GET_SIMULATION_VALUE.id, SimulationCommandHandler.instance::processGet);
		cmdMap.put(TraCICmd.SET_SIMULATION_STATE.id, SimulationCommandHandler.instance::processSet);
		cmdMap.put(TraCICmd.SUB_SIMULATION_VALUE.id, SimulationCommandHandler.instance::processValueSub);
		cmdMap.put(TraCICmd.GET_VEHICLE_VALUE.id, VehicleCommandHandler.instance::processGet);
		cmdMap.put(TraCICmd.SUB_VEHICLE_VALUE.id, VehicleCommandHandler.instance::processValueSub);
		cmdMap.put(TraCICmd.GET_POLYGON.id, PolygonCommandHandler.instance::processGet);
		cmdMap.put(TraCICmd.SUB_POLYGON_VALUE.id, PolygonCommandHandler.instance::processValueSub);
		cmdMap.put(TraCICmd.SET_VEHICLE_STATE.id, (cmd, manager) -> ((TraCISetCommand) cmd).setOK()); //todo just say ok but do nothing
	}

	public TraCIPacket execute(TraCICommand cmd) {
		TraCICmdHandler handler = cmdMap.get(cmd.getTraCICmd().id);
		int varId = cmd.getVariableIdentifier();
		if (handler == null) {
			logger.errorf("No CommandHandler found for command: %s", cmd.getTraCICmd().logShort(varId));
			return TraCIPacket.create().add_Err_StatusResponse(cmd.getTraCICmd().id, "ID not found.");
		}
		TraCIPacket response;
		try {
			logger.debugf("execute cmd: %s", cmd.getTraCICmd().logShort(varId));
			cmd = handler.handel(cmd, remoteManager);
		} catch (Exception e) {
			String err = String.format("Error handling cmd: %s", cmd.getTraCICmd().logShort(varId));
			logger.errorf(err);
			e.printStackTrace();
			return TraCIPacket.createErr(cmd.getTraCICmd().id, err + "see Vadere log for details");
		}
		try {
			logger.debugf("build response for: %s", cmd.getTraCICmd().logShort(varId));
			response = cmd.buildResponsePacket();
		} catch (Exception e) {
			String err = String.format("error building response for: %s", cmd.getTraCICmd().logShort(varId));
			logger.errorf(err);
			e.printStackTrace();
			return TraCIPacket.createErr(cmd.getTraCICmd().id, err + "see Vadere log for details");
		}

		logger.tracef("return response for: %s", cmd.getTraCICmd().logShort());
		return response;
	}
}
