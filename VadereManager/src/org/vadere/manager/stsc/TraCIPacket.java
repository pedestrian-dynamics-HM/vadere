package org.vadere.manager.stsc;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.commands.TraCICmd;
import org.vadere.manager.stsc.commands.TraCIGetCommand;
import org.vadere.util.geometry.shapes.VPoint;

import java.nio.ByteBuffer;
import java.util.List;

public class TraCIPacket {

	private TraCIWriter writer;
	private boolean emptyLengthField;
	private boolean finalized; //


	public static TraCIPacket create(){
		return new TraCIPacket().addEmptyLengthField();
	}

	public static TraCIPacket create(int packetSize){
		TraCIPacket packet = new TraCIPacket();
		packet.writer.writeInt(packetSize);
		return packet;
	}

	public static TraCIPacket sendStatusOK(TraCICmd cmd){
		TraCIPacket response = new TraCIPacket();
		response.writer.writeInt(11); // packet size (4 + 7) [4]
		response.add_OK_StatusResponse(cmd);
		response.finalizePacket();
		return response;
	}

	public static TraCIPacket sendStatus(TraCICmd cmd, TraCIStatusResponse status, String description){
		TraCIPacket response = new TraCIPacket();
		int cmdLen = 7 + response.writer.getStringByteCount(description);

		if (cmdLen > 255){
			//extended CMD
			cmdLen += 4; // add int field
			response.writer.writeInt(4 + cmdLen); // packet size (4 + cmdLen) [4]
			response.writer.writeUnsignedByte(0); // [1]
			response.writer.writeInt(cmdLen); // [4]
		} else {
			response.writer.writeInt(4 + cmdLen);  // [4]
			response.writer.writeUnsignedByte(cmdLen);  // [1]
		}
		response.writer.writeUnsignedByte(cmd.id); // [1]
		response.writer.writeUnsignedByte(status.code); // [1]
		response.writer.writeString(description); //[4 + strLen]
		response.finalizePacket();
		return response;
	}

	private void finalizePacket(){
		finalized = true;
	}

	private void throwIfFinalized(){
		if (finalized)
			throw new TraCIException("Cannot change finalized TraCIPacket");
	}

	private TraCIPacket() {
		writer = new TraCIWriterImpl();
		finalized = false;
		emptyLengthField = false;
	}



	public TraCIPacket reset(){
		writer.rest();
		return this;
	}

	private TraCIPacket addEmptyLengthField(){
		if(emptyLengthField)
			throw  new IllegalStateException("Should only be called at most once.");
		writer.writeInt(-1);
		emptyLengthField = true;
		return this;
	}


	public byte[] send() {

		// packet is valid TraCI packet an can be send.
		if (finalized)
			return writer.asByteArray();

		// packet size must be set to correct value
		if (emptyLengthField){
			ByteBuffer packet = writer.asByteBuffer();
			packet.putInt(packet.capacity());
			packet.position(0);
			return packet.array();
		} else {
			return writer.asByteArray();
		}

	}


	private TraCIWriter getCmdBuilder(TraCIGetCommand cmd, TraCICmd responseCMD){
		return new TraCIWriterImpl()
				.writeUnsignedByte(responseCMD.id)
				.writeUnsignedByte(cmd.getVariableId())
				.writeString(cmd.getElementId());
	}


	public TraCIPacket wrapGetResponse(TraCIGetCommand cmd, TraCICmd responseCMD, List<String> data){

		add_OK_StatusResponse(cmd.getTraCICmd());

		addCommand(getCmdBuilder(cmd, responseCMD)
						.writeStringListWithId(data)
						.asByteArray());
		return this;
	}


	public TraCIPacket wrapGetResponse(TraCIGetCommand cmd, TraCICmd responseCMD, int data){

		add_OK_StatusResponse(cmd.getTraCICmd());

		addCommand(getCmdBuilder(cmd, responseCMD)
				.writeIntWithId(data)
				.asByteArray());

		return this;
	}

	public TraCIPacket wrapGetResponse(TraCIGetCommand cmd, TraCICmd responseCMD, double data){

		add_OK_StatusResponse(cmd.getTraCICmd());

		addCommand(getCmdBuilder(cmd, responseCMD)
				.writeDoubleWithId(data)
				.asByteArray());

		return this;
	}

	public TraCIPacket wrapGetResponse(TraCIGetCommand cmd, TraCICmd responseCMD, VPoint data){

		add_OK_StatusResponse(cmd.getTraCICmd());

		addCommand(getCmdBuilder(cmd, responseCMD)
				.write2DPosition(data.x, data.y)
				.asByteArray());

		return this;
	}


	private void addCommand(byte[] buffer){
		if (buffer.length > 255){
			writer.writeUnsignedByte(0);
			writer.writeInt(buffer.length);
			writer.writeBytes(buffer);
		} else {
			writer.writeUnsignedByte(buffer.length);
			writer.writeBytes(buffer);
		}
	}


	public TraCIPacket add_Err_StatusResponse(int cmdIdentifier, String description){
		throwIfFinalized();
		return addStatusResponse(cmdIdentifier, TraCIStatusResponse.ERR, description);
	}

	public TraCIPacket add_OK_StatusResponse(TraCICmd traCICmd){
		throwIfFinalized();
		return add_OK_StatusResponse(traCICmd.id);
	}

	public TraCIPacket add_OK_StatusResponse(int cmdIdentifier){
		throwIfFinalized();
		// simple OK Status without description.
		writer.writeUnsignedByte(7);
		writer.writeUnsignedByte(cmdIdentifier);
		writer.writeUnsignedByte(TraCIStatusResponse.OK.code);
		writer.writeInt(0);
		return this;
	}

	public TraCIPacket addStatusResponse(int cmdIdentifier, TraCIStatusResponse response, String description){
		throwIfFinalized();
		// expect single byte cmdLenField.
		// cmdLenField + cmdIdentifier + cmdResult + strLen + str
		// 1 + 1 + 1 + 4 + len(strBytes)
		int cmdLen = 7 + writer.stringByteCount(description);

		writer.writeCommandLength(cmdLen); // 1b
		writer.writeUnsignedByte(cmdIdentifier); // 1b
		writer.writeUnsignedByte(response.code); // 4b
		writer.writeString(description); // 4b + X

		return this;
	}

	public TraCIWriter getWriter(){
		throwIfFinalized();
		return writer;
	}

	public int size(){
		return writer.size();
	}

}
