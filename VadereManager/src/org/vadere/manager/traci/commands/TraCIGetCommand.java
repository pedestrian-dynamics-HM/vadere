package org.vadere.manager.traci.commands;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.manager.traci.reader.TraCICommandBuffer;
import org.vadere.manager.traci.respons.TraCIGetResponse;

import java.nio.charset.StandardCharsets;


/**
 * Sub class of {@link TraCICommand} which represents a get request to some API.
 *
 * An API in this context is for instance the Person(GET/SET), Simulation(GET/SET/SUB)
 *
 * Command Structure
 *
 * [ cmdIdentifier(based on API) ] [ variableId ] [ elementId]
 *
 * - cmdIdentifier(based on API): see {@link TraCICmd} enum GET_****
 * - variableId: Id for the variable. The numbers may be the same between different APIs
 *   see {@link org.vadere.manager.traci.commandHandler.TraCIPersonVar} enum
 * - elementId: String based identifier for the object (i.e. a pedestrianId)
 *
 * see {@link org.vadere.manager.traci.commandHandler.PersonCommandHandler} for execution handing.
 *
 */
public class TraCIGetCommand extends TraCICommand {

	protected int variableIdentifier;
	protected String elementIdentifier;

	private TraCIGetResponse response;


	public static TraCIPacket build(TraCICmd commandIdentifier, int variableIdentifier, String elementIdentifier){
		int cmdLen = 1 + 1 + 1 + 4 + elementIdentifier.getBytes(StandardCharsets.US_ASCII).length;
		TraCIPacket packet = TraCIPacket.create();
		packet.writeCommandLength(cmdLen) // [1|5]
				.writeUnsignedByte(commandIdentifier.id) // 1
				.writeUnsignedByte(variableIdentifier) // 1
				.writeString(elementIdentifier); // 4+strLen

		return packet;
	}

	public TraCIGetCommand(TraCICmd traCICmd, int variableIdentifier, String elementIdentifier) {
		super(traCICmd);
		this.variableIdentifier = variableIdentifier;
		this.elementIdentifier = elementIdentifier;
	}

	public TraCIGetCommand(TraCICmd traCICmd, TraCICommandBuffer cmdBuffer) {
		super(traCICmd);
		variableIdentifier = cmdBuffer.readUnsignedByte();
		elementIdentifier = cmdBuffer.readString();
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
		return TraCIPacket.create().wrapGetResponse(response);
	}
}
