package org.vadere.manager.stsc;

import java.nio.ByteBuffer;

public class TraCICommandBuffer extends TraCIBuffer{

	private boolean cmdIdentifierRead;

	public static TraCICommandBuffer wrap(byte[] buf){
		return new TraCICommandBuffer(buf);
	}

	public static TraCICommandBuffer wrap(ByteBuffer buf){
		return new TraCICommandBuffer(buf);
	}

	protected TraCICommandBuffer(byte[] buf) {
		super(buf);
		cmdIdentifierRead = false;
	}

	protected TraCICommandBuffer(ByteBuffer buf) {
		super(buf);
		cmdIdentifierRead = false;
	}


	public int readCmdIdentifier(){
		if (cmdIdentifierRead)
			throw new IllegalStateException("TraCI Command Identifier already consumed. readCmdIdentifier() must only be called once. Something went wrong in the TraCI message handling.");

		cmdIdentifierRead = true;
		return buffer.readUnsignedByte();
	}


}
