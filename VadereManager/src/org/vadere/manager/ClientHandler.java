package org.vadere.manager;

import org.vadere.manager.traci.commandHandler.CommandExecutor;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.reader.TraCIPacketBuffer;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.util.logging.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;

/**
 * //todo comment
 */
public class ClientHandler implements Runnable {

	private static Logger logger = Logger.getLogger(ClientHandler.class);

	private final ServerSocket serverSocket;
	private final TraCISocket traCISocket;
	private CommandExecutor cmdExecutor;
	private RemoteManager remoteManager;
	private String scenarioString;


	public ClientHandler(ServerSocket serverSocket, TraCISocket traCISocket, Path basedir, boolean guiSupport) {
		this.serverSocket = serverSocket;
		this.traCISocket = traCISocket;
		this.remoteManager = new RemoteManager(basedir, guiSupport);
		this.cmdExecutor = new CommandExecutor(remoteManager);
		this.scenarioString = ""; // traci will provide the scenario
	}

	public void setScenario(String scenarioString) {
		this.scenarioString = scenarioString;
	}

	@Override
	public void run() {
		try {
			if (!scenarioString.equals("")){
				// scenario provided in command line. Load it and then wait for traci commands
				remoteManager.loadScenario(this.scenarioString);
				remoteManager.startSimulation();
			}
			handleClient();
		} catch (EOFException eof) {
			logger.infof("EOF. Client closed socket");
		} catch (IOException io) {
			logger.error("Exception caught when trying to listen on port "
					+ 9999 + " or listening for a connection", io);
		} catch (Exception e) {
			logger.error("Error while handling TraCI Message", e);
		}
	}

	private void handleClient() throws IOException {
		try {
			logger.info("client connected...");

			while (true) {

				TraCIPacketBuffer traCIPacketBuffer = traCISocket.receiveExact();

				if (traCIPacketBuffer.hasRemaining()) {
					TraCICommand cmd = traCIPacketBuffer.nextCommand();
					while (cmd != null) {

						TraCIPacket response = cmdExecutor.execute(cmd);
						logger.debugf("send packet [%d byte]", response.size());
						traCISocket.sendExact(response);

						cmd = traCIPacketBuffer.nextCommand();
					}
				}

			}
		}
		finally {
			traCISocket.close();
			remoteManager.stopSimulationIfRunning();
			cmdExecutor = null;
			remoteManager = null;
			// hint VM to call garbage collection. The current simulation is done.
			System.gc();
		}

	}

}
