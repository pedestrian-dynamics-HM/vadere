package org.vadere.manager.stsc;

import java.nio.ByteBuffer;

public interface ByteWriter {

	void writeByte(int val);

	default void writeUnsignedByte(int val){
		if (val>= 0 && val<=255){
			writeByte(val);
		} else {
			throw new IllegalArgumentException(
					"unsignedByte must be within (including) 0..255 but was: " + val);
		}
	}
	void writeBytes(byte[] buf);
	void writeBytes(byte[] buf, int offset, int len);

	default void writeBytes(ByteBuffer buf, int offset, int len){
		writeBytes(buf.array(), offset, len);
	}
	default void writeBytes(ByteBuffer buf){
		writeBytes(buf, 0, buf.array().length);
	}

	default void writeInt(int val){
		writeBytes(ByteBuffer.allocate(4).putInt(val).array());
	}

	default void writeDouble(double val){
		writeBytes(ByteBuffer.allocate(8).putDouble(val).array());
	}

}