package org.vadere.manager.stsc.commands.control;

import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.writer.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;
import org.vadere.manager.stsc.respons.TraCISimTimeResponse;

public class TraCISimStepCommand extends TraCICommand {

	private double targetTime;
	private TraCISimTimeResponse response;

	public static TraCIPacket build(double targetTime){
		TraCIPacket packet = TraCIPacket.create(14); // 4
		packet.writeUnsignedByte(10) // 1
				.writeUnsignedByte(TraCICmd.SIM_STEP.id) // 1
				.writeDouble(targetTime); // 8
		return packet;
	}

	public TraCISimStepCommand(TraCICommandBuffer cmdBuffer) {
		super(TraCICmd.SIM_STEP);
		this.targetTime = cmdBuffer.readDouble();
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
			return TraCIPacket.create().wrapSimTimeStepCommand(this); // TODO
	}
}
