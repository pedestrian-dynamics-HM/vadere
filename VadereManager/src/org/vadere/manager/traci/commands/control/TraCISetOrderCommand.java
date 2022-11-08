package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.reader.TraCICommandBuffer;
import org.vadere.manager.traci.writer.TraCIPacket;

public class TraCISetOrderCommand extends TraCICommand {

	public TraCISetOrderCommand(TraCICommandBuffer cmdBuffer) {
		super(TraCICmd.SET_ORDER);
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		if (NOK_response != null)
			return NOK_response;
		else
			return TraCIPacket.create(11).add_OK_StatusResponse(TraCICmd.SET_ORDER);
	}
    
}
