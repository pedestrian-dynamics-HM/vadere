package org.vadere.manager.stsc.commands.control;

import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.commands.TraCICommand;

public class TraCICloseCommand extends TraCICommand {

	public TraCICloseCommand(TraCICmd traCICmd) {
		super(traCICmd);
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		return TraCIPacket.create(11).add_OK_StatusResponse(TraCICmd.CLOSE);
	}

}
