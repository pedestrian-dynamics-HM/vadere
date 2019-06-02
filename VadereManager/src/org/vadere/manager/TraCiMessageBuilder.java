package org.vadere.manager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TraCiMessageBuilder {

	protected ByteArrayOutputStream byteArrayOutput;
	TraCiMessageBuilder(){
		reset();
	}

	void reset() {
		byteArrayOutput = new ByteArrayOutputStream();
		// add 4 byte at the beginning as a placeholder for message size.
		writeInt(0);
	}

	public ByteBuffer build(){
		// prepend size of message.
		ByteBuffer buffer = ByteBuffer.wrap(byteArrayOutput.toByteArray());
		buffer.putInt(byteArrayOutput.size());
		buffer.position(0);
		return buffer;
	}

	public TraCiMessageBuilder writeUnsignedByte(int b) {

		if(b >= 0 && b <= 255){
			byteArrayOutput.write(b);
		} else {
			throw new IllegalArgumentException("unsigned byte must be within 0..255");
		}

		return this;
	}

	public TraCiMessageBuilder writeInt(int i){
		byteArrayOutput.writeBytes(ByteBuffer.allocate(4).putInt(i).array());
		return this;
	}


	public TraCiMessageBuilder writeBytes(ByteBuffer data){
		byteArrayOutput.write(data.array(), data.arrayOffset(), data.array().length);
		return this;
	}

	public TraCiMessageBuilder writeBytes(byte[] data){
		byteArrayOutput.writeBytes(data);
		return this;
	}

	public TraCiMessageBuilder writeStringASCII(String s){
		byte[] stringBytes;
		stringBytes = s.getBytes(StandardCharsets.US_ASCII);

		writeInt(stringBytes.length);
		writeBytes(stringBytes);
		return this;
	}


}