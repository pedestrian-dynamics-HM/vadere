package org.vadere.manager;

import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.reader.TraCIPacketBuffer;
import org.vadere.manager.stsc.respons.TraCIResponse;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

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

	// send //

	private void send(final byte[] buf) throws IOException {
		outStream.write(buf);
	}

	private void send(ByteBuffer buf) throws IOException {
		outStream.write(buf.array(), buf.arrayOffset(), buf.array().length);
	}

	public void sendExact(final TraCIPacket packet) throws IOException{
		send(packet.send());
	}


	// receive //

	public void receiveComplete(byte[] buf, int len) throws IOException {
		inStream.readFully(buf, 0, len);
	}

	public byte[] receive (int bufSize) throws IOException{
		byte[] buf = new byte[bufSize];
		receiveComplete(buf, bufSize);
		return buf;
	}

	public TraCIPacketBuffer receiveExact() throws IOException{

		// read first 4 bytes (containing TracCI packet length)
		ByteBuffer msgLength = ByteBuffer.wrap(receive(TRACI_LEN_LENGTH));
		int data_length = msgLength.getInt() - TRACI_LEN_LENGTH;

		if (data_length <=0){
			return TraCIPacketBuffer.empty();
		} else {
			byte[] data = receive(data_length);
			return  TraCIPacketBuffer.wrap(data);
		}
	}

	public TraCIResponse receiveResponse() throws IOException {
		TraCIPacketBuffer buf = receiveExact();
		return buf.nextResponse();
	}


	@Override
	public void close() throws IOException {
		socket.close();
	}
}
