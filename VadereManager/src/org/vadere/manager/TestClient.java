package org.vadere.manager;

import de.tudresden.sumo.util.CommandProcessor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TestClient {

	private int port;
	private  Socket socket;
	private CommandProcessor cmdProcessor;


	public TestClient(int port) throws IOException, InterruptedException {
		this.port = port;

		socket = new Socket();
		socket.setTcpNoDelay(true);
		int waitTime = 500; //ms
		for(int i=0; i < 14; i++){
			try {
				socket.connect(new InetSocketAddress("127.0.0.1", this.port));
				break;
			} catch (ConnectException ex){
				Thread.sleep(waitTime);
				waitTime *= 2;
			}
		}

		if (!socket.isConnected()){
			throw new IOException("can't connect to Server!");
		} else {
			cmdProcessor = new CommandProcessor(socket);
		}
		System.out.println("connected...");
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		TestClient testClient = new TestClient(9999);

		testClient.cmdProcessor.do_setOrder(99);

	}


}
