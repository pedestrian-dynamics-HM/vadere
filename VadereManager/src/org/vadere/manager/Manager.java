package org.vadere.manager;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Manager {


	public static void main(String[] args) throws IOException {

		ExecutorService pool = Executors.newFixedThreadPool(4);
		ServerSocket serverSocket = new ServerSocket(9999);

		VadereServer server = new VadereServer(serverSocket, pool);
		server.run();

	}


}
