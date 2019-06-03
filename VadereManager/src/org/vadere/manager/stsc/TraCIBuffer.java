package org.vadere.manager.stsc;

import java.nio.ByteBuffer;

public abstract class TraCIBuffer {

	protected TraCIReader buffer;

	protected TraCIBuffer (byte[] buf){
		buffer = TraCIReader.wrap(buf);
	}

	protected TraCIBuffer (ByteBuffer buf){
		buffer = TraCIReader.wrap(buf);
	}


	public boolean hasRemaining(){
		return buffer.hasRemaining();
	}
}
