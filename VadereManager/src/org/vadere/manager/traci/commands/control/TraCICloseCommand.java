package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.respons.StatusResponse;
import org.vadere.manager.traci.respons.TraCIResponse;
import org.vadere.manager.traci.respons.TraCIStatusResponse;

public class TraCICloseCommand extends TraCICommand {

	private TraCIResponse response;

	public static TraCIPacket build(){

		TraCIPacket packet = TraCIPacket.create(6); // 4
		packet.writeUnsignedByte(2) // 1
				.writeUnsignedByte(TraCICmd.CLOSE.id); // 1

		return packet;
	}

	public TraCICloseCommand() {
		super(TraCICmd.CLOSE);
		this.response = new TraCIResponse(
				new StatusResponse(TraCICmd.CLOSE, TraCIStatusResponse.OK, ""),
				TraCICmd.CLOSE);
	}

	public TraCIResponse getResponse() {
		return response;
	}

	public void setResponse(TraCIResponse response) {
		this.response = response;
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		if (NOK_response != null)
			return NOK_response;
		else
			return TraCIPacket.create().addStatusResponse(response.getStatusResponse());
	}

}
