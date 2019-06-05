package org.vadere.manager;

import de.tudresden.sumo.config.Constants;

import org.vadere.manager.commandHandler.ControlCommandHandler;
import org.vadere.manager.commandHandler.PersonCommandHandler;
import org.vadere.manager.commandHandler.TraCICmdHandler;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.util.logging.Logger;

import java.util.HashMap;

public class CommandExecutor {

	private static Logger logger = Logger.getLogger(CommandExecutor.class);

	private HashMap<Integer, TraCICmdHandler> cmdMap;

	public CommandExecutor() {
		cmdMap = new HashMap<>();
		cmdMap.put(Constants.CMD_GETVERSION, ControlCommandHandler.instance::process_getVersion);
		cmdMap.put(Constants.CMD_GET_PERSON_VARIABLE, PersonCommandHandler.instance::processGet);
		cmdMap.put(Constants.CMD_SET_PERSON_VARIABLE, PersonCommandHandler.instance::processSet);
//		cmdMap.put(Constants.CMD_GET_SIM_VARIABLE, SimulationCommands::processGet);
//		cmdMap.put(Constants.CMD_SET_SIM_VARIABLE, SimulationCommands::processSet);
//		cmdMap.put(Constants.CMD_GET_POLYGON_VARIABLE, SimulationCommands::processGet);
//		cmdMap.put(Constants.CMD_SET_POLYGON_VARIABLE, SimulationCommands::processSet);
	}

	TraCIPacket execute(TraCICommand cmd){
		TraCICmdHandler handler = cmdMap.get(cmd.getTraCICmd());
		if (handler == null){
			logger.errorf("No CommandHandler found for command: %02X", cmd.getTraCICmd());
			return TraCIPacket.create().add_Err_StatusResponse(cmd.getTraCICmd().id, "ID not found.");
		}

		return handler.handel(cmd);

	}
}
