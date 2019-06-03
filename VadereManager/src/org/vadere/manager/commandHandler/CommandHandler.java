package org.vadere.manager.commandHandler;

import org.vadere.manager.TraCICommand;
import org.vadere.manager.stsc.TraCIPacket;

public interface CommandHandler {

	TraCIPacket handelCommand(TraCICommand cmd);

}
