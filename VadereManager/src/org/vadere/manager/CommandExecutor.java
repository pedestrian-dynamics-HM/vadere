package org.vadere.manager;

import de.tudresden.sumo.config.Constants;

import org.vadere.manager.commandHandler.CommandHandler;
import org.vadere.manager.commandHandler.PersonCommandHandler;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.util.logging.Logger;

import java.util.HashMap;

public class CommandExecutor {

	private static Logger logger = Logger.getLogger(CommandExecutor.class);

	private HashMap<Integer, CommandHandler> cmdMap;

	public CommandExecutor() {
		cmdMap = new HashMap<>();
//		cmdMap.put(Constants.CMD_GETVERSION, ControlCommands::getVersion);
		cmdMap.put(Constants.CMD_GET_PERSON_VARIABLE, new PersonCommandHandler());
		cmdMap.put(Constants.CMD_SET_PERSON_VARIABLE, new PersonCommandHandler());
//		cmdMap.put(Constants.CMD_GET_SIM_VARIABLE, SimulationCommands::processGet);
//		cmdMap.put(Constants.CMD_SET_SIM_VARIABLE, SimulationCommands::processSet);
//		cmdMap.put(Constants.CMD_GET_POLYGON_VARIABLE, SimulationCommands::processGet);
//		cmdMap.put(Constants.CMD_SET_POLYGON_VARIABLE, SimulationCommands::processSet);
	}

	TraCIPacket execute(TraCICommand cmd){
		CommandHandler handler = cmdMap.get(cmd.getTraCICmd());
		if (handler == null){
			logger.errorf("No CommandHandler found for command: %02X", cmd.getTraCICmd());
			return TraCIPacket.createDynamicPacket().add_Err_StatusResponse(cmd.getTraCICmd().id, "ID not found.");
		}

		return handler.handel(cmd);

	}
}
