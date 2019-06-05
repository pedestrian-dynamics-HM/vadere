package org.vadere.manager.commandHandler;

import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICommand;

public interface TraCICmdHandler {


	TraCIPacket handel (TraCICommand cmd);

}
