package org.vadere.manager;

import java.nio.ByteBuffer;

public class TraciMessageBuffer {

	ByteBuffer data;


	public static TraciMessageBuffer wrap(byte[] buf){
		TraciMessageBuffer msgBuf = new TraciMessageBuffer();
		msgBuf.data = ByteBuffer.wrap(buf);
		return msgBuf;
	}

	private TraciMessageBuffer() { }


	/**
	 * Ensure that at least n bytes can be read from the Buffer.
	 * @param n	number of bytes
	 */
	public boolean ensureBytes(int n){
		return (data.limit() - data.position()) >= n;
	}

	public boolean reachedEnd(){
		return data.position() == data.limit();
	}


	public ByteBuffer readBytes(int n){
		return data.get(new byte[n]);
	}

	public byte readByte(){
		return data.get();
	}

	public int readUnsignedByte(){
		return data.get() & 0xff; // get byte, cast to int and return the last byte (unsigned)
	}

	public int readInt(){
			return data.getInt();
	}



}
