package org.vadere.manager.stsc;

import java.nio.ByteBuffer;

public interface ByteReader {


	byte readByte();
	default int readUnsignedByte(){
		// (signed)byte --cast--> (signed)int --(& 0xff)--> cut highest three bytes.
		// This result represents the an unsigned byte value (0..255) as an int.
		return (int)readByte() & 0xff;
	}

	byte[] readBytes(int num);
	default ByteBuffer readByteBuffer(int num){
		return ByteBuffer.wrap(readBytes(num));
	}

	int readInt();
	double readDouble();
}
