package org.vadere.manager;

import de.tudresden.sumo.config.Constants;

import org.vadere.manager.commandHandler.CommandHandler;
import org.vadere.manager.commandHandler.GetVersionCmdHandler;
import org.vadere.util.logging.Logger;

import java.util.HashMap;

public class CommandExecutor {

	private static Logger logger = Logger.getLogger(CommandExecutor.class);

	private HashMap<Integer, CommandHandler> cmdMap;

	public CommandExecutor() {
		cmdMap = new HashMap<>();
		cmdMap.put(Constants.CMD_GETVERSION, new GetVersionCmdHandler());
	}

	boolean execute(TraciCommand cmd, TraCiMessageBuilder builder){
		CommandHandler handler = cmdMap.get(cmd.getId());
		if (handler == null){
			logger.errorf("No CommandHandler found for command: %02X", cmd);
			return false;
		}

		return handler.handelCommand(cmd, builder);

	}
}
