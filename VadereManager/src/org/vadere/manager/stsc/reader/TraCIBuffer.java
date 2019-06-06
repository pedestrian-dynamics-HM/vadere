package org.vadere.manager.stsc.reader;

import java.nio.ByteBuffer;

/**
 * Wrapper around byte representation for packets, commands and responses
 * received over a socket.
 */
public abstract class TraCIBuffer {

	public TraCIReader reader;

	protected TraCIBuffer (byte[] buf){
		reader = TraCIReaderImpl.wrap(buf);
	}

	protected TraCIBuffer (ByteBuffer buf){
		reader = TraCIReaderImpl.wrap(buf);
	}


	public boolean hasRemaining(){
		return reader.hasRemaining();
	}
}
