package org.vadere.manager.stsc.commands;

import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;
import org.vadere.manager.stsc.respons.TraCIGetResponse;

import java.nio.charset.StandardCharsets;

public class TraCIGetCommand extends TraCICommand {

	protected int variableIdentifier;
	protected String elementIdentifier;

	private TraCIGetResponse response;


	public static TraCIPacket build(TraCICmd commandIdentifier, int variableIdentifier, String elementIdentifier){
		int cmdLen = 1 + 1 + 1 + 4 + elementIdentifier.getBytes(StandardCharsets.US_ASCII).length;
		TraCIPacket packet = TraCIPacket.create();
		packet.getWriter()
				.writeCommandLength(cmdLen) // [1|5]
				.writeUnsignedByte(commandIdentifier.id) // 1
				.writeUnsignedByte(variableIdentifier) // 1
				.writeString(elementIdentifier); // 4+strLen

		return packet;
	}

	public TraCIGetCommand(TraCICmd traCICmd, TraCICommandBuffer cmdBuffer) {
		super(traCICmd);
		variableIdentifier = cmdBuffer.reader.readUnsignedByte();
		elementIdentifier = cmdBuffer.reader.readString();
	}

	public int getVariableIdentifier() {
		return variableIdentifier;
	}

	public void setVariableIdentifier(int variableIdentifier) {
		this.variableIdentifier = variableIdentifier;
	}

	public String getElementIdentifier() {
		return elementIdentifier;
	}

	public void setElementIdentifier(String elementIdentifier) {
		this.elementIdentifier = elementIdentifier;
	}

	public TraCIGetResponse getResponse() {
		return response;
	}

	public void setResponse(TraCIGetResponse response) {
		response.setVariableIdentifier(variableIdentifier);
		response.setElementIdentifier(elementIdentifier);
		this.response = response;
	}



	@Override
	public TraCIPacket buildResponsePacket() {
		if (NOK_response != null)
			return NOK_response;
		else
			return TraCIPacket.create().wrapGetResponse(response);
	}
}
