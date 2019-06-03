package org.vadere.manager.stsc;

import org.vadere.manager.TraCICommand;

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
		if (!buffer.hasRemaining())
			return null;

		int cmdLen = getCommandDataLen();
		return new TraCICommand(TraCICommandBuffer.wrap(buffer.readByteBuffer(cmdLen)));
	}


	private int getCommandDataLen(){
		int cmdLen = buffer.readUnsignedByte();
		if (cmdLen == 0 ){
			// extended cmdLen field used.
			cmdLen = buffer.readInt() - 5; // subtract cmdLen field:  1 ubyte + 1 int (4)
		} else {
			cmdLen -= 1; // subtract cmdLen field: 1 ubyte
		}
		return cmdLen;
	}
}
