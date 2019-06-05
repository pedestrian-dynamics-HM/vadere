package org.vadere.manager.stsc;

import org.vadere.manager.stsc.commands.TraCICommand;

import java.nio.ByteBuffer;

/**
 *  Contains a {@link ByteBuffer} which encodes
 *  possible multiple commands.
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

		return TraCICommand.createCommand(reader.readByteBuffer(cmdLen));
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
