package org.vadere.manager.stsc.reader;

import org.vadere.manager.stsc.respons.StatusResponse;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.respons.TraCIResponse;

import java.nio.ByteBuffer;

/**
 *  Wraps the whole packet received over the socket with packet length removed.
 */
public class TraCIPacketBuffer extends TraCIBuffer {

	public  static TraCIPacketBuffer wrap(byte[] buf){
		return new TraCIPacketBuffer(buf);
	}

	public  static TraCIPacketBuffer wrap(ByteBuffer buf){
		return new TraCIPacketBuffer(buf);
	}

	public static TraCIPacketBuffer empty(){
		return new TraCIPacketBuffer(new byte[0]);
	}

	protected TraCIPacketBuffer(byte[] buf){
		super(buf);
	}

	protected TraCIPacketBuffer(ByteBuffer buf){
		super(buf);
	}

	public TraCICommand nextCommand(){
		if (!reader.hasRemaining())
			return null;

		int cmdLen = getCommandDataLen();

		return TraCICommand.create(reader.readByteBuffer(cmdLen));
	}

	public TraCIResponse nextResponse(){
		if (!reader.hasRemaining())
			return null;

		int statusLen = getCommandDataLen();
		StatusResponse statusResponse = StatusResponse.createFromByteBuffer(reader.readByteBuffer(statusLen));

		if (!reader.hasRemaining()){
			// only StatusResponse
			return TraCIResponse.create(statusResponse);
		} else {
			int responseDataLen = getCommandDataLen();
			ByteBuffer buffer = reader.readByteBuffer(responseDataLen);
			return TraCIResponse.create(statusResponse, buffer);
		}
	}

	private int getCommandDataLen(){
		int cmdLen = reader.readUnsignedByte();
		if (cmdLen == 0 ){
			// extended cmdLen field used.
			cmdLen = reader.readInt() - 5; // subtract cmdLen field:  1 ubyte + 1 int (4)
		} else {
			cmdLen -= 1; // subtract cmdLen field: 1 ubyte
		}
		return cmdLen;
	}
}
