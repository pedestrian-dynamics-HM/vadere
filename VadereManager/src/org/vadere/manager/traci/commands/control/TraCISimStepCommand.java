package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.reader.TraCICommandBuffer;
import org.vadere.manager.traci.response.TraCISimTimeResponse;
import org.vadere.manager.traci.writer.TraCIPacket;

public class TraCISimStepCommand extends TraCICommand {

	private double targetTime;
	private TraCISimTimeResponse response;

	public TraCISimStepCommand(TraCICommandBuffer cmdBuffer) {
		super(TraCICmd.SIM_STEP);
		this.targetTime = cmdBuffer.readDouble();
	}

	public static TraCIPacket build(double targetTime) {
		TraCIPacket packet = TraCIPacket.create(14); // 4
		packet.writeUnsignedByte(10) // 1
				.writeUnsignedByte(TraCICmd.SIM_STEP.id) // 1
				.writeDouble(targetTime); // 8
		return packet;
	}

	public double getTargetTime() {
		return targetTime;
	}

	public void setTargetTime(double targetTime) {
		this.targetTime = targetTime;
	}

	public TraCISimTimeResponse getResponse() {
		return response;
	}

	public void setResponse(TraCISimTimeResponse response) {
		this.response = response;
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		if (NOK_response != null)
			return NOK_response;
		else
			return TraCIPacket.create().wrapSimTimeStepCommand(response); // TODO
	}
}
