package org.vadere.manager.server;

import org.vadere.manager.ClientHandler;
import org.vadere.manager.TraCISocket;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

/**
 * Open socket and wait for one client. After the simulation is finished do not accept new scenario
 * files and stop gracefully.
 */
public class VadereSingleClientServer extends AbstractVadereServer {


	private String scenarioPath;

	public VadereSingleClientServer(ServerSocket serverSocket, Path baseDir, boolean guiSupport, boolean trace, String scenarioPath) {
		super(serverSocket, baseDir, guiSupport, trace);
		this.scenarioPath = scenarioPath;
	}

	@Override
	public void run() {
		try {
			logger.infof("listening on port %d... (gui-mode: %s) Single Simulation", serverSocket.getLocalPort(), Boolean.toString(guiSupport));
			Socket clientSocket = serverSocket.accept();

			ClientHandler handler = new ClientHandler(serverSocket, new TraCISocket(clientSocket, trace), baseDir, guiSupport);
			if (scenarioPath != null){
				if (!scenarioPath.equals("")){
					handler.setScenario(IOUtils.readTextFile(scenarioPath));
				}
			}

			Thread traciThread = new Thread(handler);
			traciThread.start();
			traciThread.join();

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
