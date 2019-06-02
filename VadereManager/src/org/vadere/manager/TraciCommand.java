package org.vadere.manager;

import java.nio.ByteBuffer;
import java.util.Queue;

public class TraciCommand {

	private int id;
	private ByteBuffer data;

	public TraciCommand(int id, ByteBuffer data) {
		this.id = id;
		this.data = data;
	}

	static void extractCommandsFromByteBuffer(Queue<TraciCommand> queue, TraciMessageBuffer buf){

		while (!buf.reachedEnd()){
			int dataLen = buf.readUnsignedByte();
			if (dataLen == 0) { // extended command length field used.
				if (!buf.ensureBytes(4))
					throw new TraCiException("expected extended command format but TraciMessageBuffer is empty.");
				dataLen = buf.readInt();
				dataLen -= 6; // (1 + 4) + 1 (ext. command length + id)
			} else {
				dataLen -= 2; // 1 + 1 (command length field + id)
			}

			int cmdId = buf.readUnsignedByte();
			ByteBuffer data = dataLen > 0 ? buf.readBytes(dataLen) : null;

			queue.add(new TraciCommand(cmdId, data));
		}
	}


	public int getId() {
		return id;
	}

	public ByteBuffer getData() {
		return data;
	}

}
