package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.response.TraCIGetVersionResponse;
import org.vadere.manager.traci.writer.TraCIPacket;

public class TraCIGetVersionCommand extends TraCICommand {

	private TraCIGetVersionResponse response;

	public TraCIGetVersionCommand() {
		super(TraCICmd.GET_VERSION);
		response = new TraCIGetVersionResponse();
	}

	public static TraCIPacket build() {
		TraCIPacket packet = TraCIPacket.create(6); // 4
		packet.writeCommandLength(2) // 1
				.writeUnsignedByte(TraCICmd.GET_VERSION.id); // 1
		return packet;
	}

	public TraCIGetVersionResponse getResponse() {
		return response;
	}

	public void setResponse(TraCIGetVersionResponse response) {
		this.response = response;
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		if (NOK_response != null)
			return NOK_response;
		else
			return TraCIPacket.create().wrapGetVersionCommand(this);
	}


}
