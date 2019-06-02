package org.vadere.manager;

import de.uniluebeck.itm.tcpip.Storage;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Queue;

public class TraCISocket implements Closeable {

	private final static int TRACI_LEN_LENGTH = 4;

	private final Socket socket;
	private final DataOutputStream outStream;
	private final DataInputStream inStream;


	public TraCISocket(Socket socket) throws IOException {
		this.socket = socket;
		this.outStream = new DataOutputStream(socket.getOutputStream());
		this.inStream = new DataInputStream(socket.getInputStream());
	}

	public int getPort(){
		return socket.getPort();
	}

	public boolean hasClientConnection(){
		return socket.isConnected();
	}

	public void send(final byte[] buf) throws IOException {
		outStream.write(buf);
	}

	public void send(ByteBuffer buf) throws IOException {
		outStream.write(buf.array(), buf.arrayOffset(), buf.array().length);
	}

	public void sendExact(final Storage storage) throws IOException{
		int totalLength = TRACI_LEN_LENGTH + storage.size();
		Storage length_storage = new Storage();
		length_storage.writeInt(totalLength);

		byte[] data = new byte[totalLength];
		int n = 0;
		// write length of total package
		for(Byte b : length_storage.getStorageList()){
			data[n++] = b;
		}
		// write data
		for(Byte b : storage.getStorageList()){
			data[n++] = b;
		}

		send(data);
	}



	public void receiveComplete(byte[] buf, int len) throws IOException {
		inStream.readFully(buf, 0, len);
	}

	public byte[] receive (int bufSize) throws IOException{
		byte[] buf = new byte[bufSize];
		receiveComplete(buf, bufSize);
		return buf;
	}

	public boolean receiveExact(Queue<TraciCommand> queue) throws IOException{

		ByteBuffer msgLength = ByteBuffer.wrap(receive(TRACI_LEN_LENGTH));
		int data_length = msgLength.getInt() - TRACI_LEN_LENGTH;
		assert (data_length > 0);

		// copy message content into msg.
		TraciMessageBuffer commands = TraciMessageBuffer.wrap(receive(data_length));
		TraciCommand.extractCommandsFromByteBuffer(queue, commands);
		return true;
	}


	@Override
	public void close() throws IOException {
		socket.close();
	}
}
