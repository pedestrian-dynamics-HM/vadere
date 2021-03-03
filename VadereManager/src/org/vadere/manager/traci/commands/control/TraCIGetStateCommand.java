package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.reader.TraCICommandBuffer;
import org.vadere.manager.traci.response.TraCIGetStateResponse;
import org.vadere.manager.traci.writer.TraCIPacket;

public class TraCIGetStateCommand extends TraCICommand {

	private double targetTime;
	private TraCIGetStateResponse response;

	public TraCIGetStateCommand(TraCICommandBuffer cmdBuffer) {
		super(TraCICmd.GET_STATE);
		this.targetTime = cmdBuffer.readDouble();
	}

	public static TraCIPacket build(double targetTime) {
		TraCIPacket packet = TraCIPacket.create(14); // 4
		packet.writeUnsignedByte(10) // 1
				.writeUnsignedByte(TraCICmd.GET_STATE.id) // 1
				.writeDouble(targetTime); // 8
		return packet;
	}

	public double getTargetTime() {
		return targetTime;
	}

	public void setTargetTime(double targetTime) {
		this.targetTime = targetTime;
	}

	public TraCIGetStateResponse getResponse() {
		return response;
	}

	public void setResponse(TraCIGetStateResponse response) {
		this.response = response;
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		if (NOK_response != null)
			return NOK_response;
		else
			return TraCIPacket.create().wrapGetStateCommand(response); // TODO
	}
}
