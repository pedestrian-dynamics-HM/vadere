package org.vadere.manager.stsc.commands.control;

import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICmd;
import org.vadere.manager.stsc.commands.TraCICommand;

public class CmdClose  extends TraCICommand {

	public CmdClose(TraCICmd traCICmd) {
		super(traCICmd);
	}

	@Override
	public TraCIPacket handleCommand(TraCIPacket response) {
		return null;
	}
}
