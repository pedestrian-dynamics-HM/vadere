package org.vadere.manager.stsc.commands.control;

import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.respons.TraCIGetVersionResponse;

public class TraCIGetVersionCommand extends TraCICommand {

	private TraCIGetVersionResponse response;

	public static TraCIPacket build(){
		TraCIPacket packet = TraCIPacket.create(6); // 4
		packet.getWriter()
				.writeCommandLength(2) // 1
				.writeUnsignedByte(TraCICmd.GET_VERSION.id); // 1
		return packet;
	}

	public TraCIGetVersionCommand(){
		super(TraCICmd.GET_VERSION);
		response = new TraCIGetVersionResponse();
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
