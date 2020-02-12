package org.vadere.manager.server;

import org.vadere.manager.ClientHandler;
import org.vadere.manager.TraCISocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

/**
 * Open socket and wait for one client. After the simulation is finished do not accept new scenario
 * files and stop gracefully.
 */
public class VadereSingleClientServer extends AbstractVadereServer {


	public VadereSingleClientServer(ServerSocket serverSocket, Path baseDir, boolean guiSupport, boolean trace) {
		super(serverSocket, baseDir, guiSupport, trace);
	}

	@Override
	public void run() {
		try {
			logger.infof("listening on port %d... (gui-mode: %s) Single Simulation", serverSocket.getLocalPort(), Boolean.toString(guiSupport));
			Socket clientSocket = serverSocket.accept();
			Thread t = new Thread(new ClientHandler(serverSocket, new TraCISocket(clientSocket, trace), baseDir, guiSupport));
			t.start();
			t.join();

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			logger.warn("Interrupt Vadere Server");

		} finally {
			logger.info("Shutdown Vadere Server ...");
			if (!serverSocket.isClosed()) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
