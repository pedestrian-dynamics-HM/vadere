package org.vadere.manager.stsc;

import java.nio.ByteBuffer;

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
