package org.vadere.manager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class TraCiCommandBuilder extends TraCiMessageBuilder {

	public TraCiCommandBuilder(){
		reset();
	}

	@Override
	void reset() {
		byteArrayOutput = new ByteArrayOutputStream();
		// add 5 byte at the beginning as a placeholder for message size.
		writeUnsignedByte(0);
		writeInt(0);
	}

	@Override
	public ByteBuffer build(){
		byte[] msg = byteArrayOutput.toByteArray();
		reset();

		ByteBuffer buffer;
		if ((msg.length - 4 ) > 255){
			// use extended
			buffer = ByteBuffer.wrap(msg);
			buffer.put((byte) 0);
			buffer.putInt(msg.length);
			buffer.position(0);
		} else {
			buffer = ByteBuffer.wrap(msg,4, msg.length-4);
			buffer.put((byte) (msg.length-4));
			buffer.position(4);
		}

		return buffer;
	}


}