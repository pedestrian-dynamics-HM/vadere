package org.vadere.manager.traci.reader;

import org.vadere.state.traci.TraCIExceptionInternal;

import java.nio.ByteBuffer;

/**
 * A simple Wrapper around a {@link TraCIReader} which knows how to traverse a single command.
 *
 * The class expects that the given buffer only contains *one* command. The command length filed (1
 * byte or 5 bytes, depending on the command limit) must be removed before creating an instance.
 */
public class TraCICommandBuffer extends TraCIByteBuffer {

	private boolean cmdIdentifierRead;

	private TraCICommandBuffer(byte[] buf) {
		super(buf);
		cmdIdentifierRead = false;
	}

	private TraCICommandBuffer(ByteBuffer buf) {
		super(buf);
		cmdIdentifierRead = false;
	}

	public static TraCICommandBuffer wrap(byte[] buf) {
		return new TraCICommandBuffer(buf);
	}

	public static TraCICommandBuffer wrap(ByteBuffer buf) {
		return new TraCICommandBuffer(buf);
	}

	public static TraCICommandBuffer empty() {
		return new TraCICommandBuffer(new byte[0]);
	}

	public int readCmdIdentifier() {
		if (cmdIdentifierRead)
			throw new TraCIExceptionInternal("TraCI Command Identifier already consumed. readCmdIdentifier() must only be called once. Something went wrong in the TraCI message handling.");

		cmdIdentifierRead = true;
		return readUnsignedByte();
	}


}
