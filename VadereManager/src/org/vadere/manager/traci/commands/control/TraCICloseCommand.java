package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.response.StatusResponse;
import org.vadere.manager.traci.response.TraCIResponse;
import org.vadere.manager.traci.response.TraCIStatusResponse;
import org.vadere.manager.traci.writer.TraCIPacket;

public class TraCICloseCommand extends TraCICommand {

	private TraCIResponse response;

	public TraCICloseCommand() {
		super(TraCICmd.CLOSE);
		this.response = new TraCIResponse(
				new StatusResponse(TraCICmd.CLOSE, TraCIStatusResponse.OK, ""),
				TraCICmd.CLOSE);
	}

	public static TraCIPacket build() {

		TraCIPacket packet = TraCIPacket.create(6); // 4
		packet.writeUnsignedByte(2) // 1
				.writeUnsignedByte(TraCICmd.CLOSE.id); // 1

		return packet;
	}

	public TraCIResponse getResponse() {
		return response;
	}

	public void setResponse(TraCIResponse response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return String.format("[ %s | %s ]", traCICmd.toString(), response.toString());
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		if (NOK_response != null)
			return NOK_response;
		else
			return TraCIPacket.create().addStatusResponse(response.getStatusResponse());
	}

}
