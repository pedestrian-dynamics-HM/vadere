package org.vadere.manager;

import org.vadere.manager.commandHandler.CommandExecutor;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.reader.TraCIPacketBuffer;
import org.vadere.manager.stsc.writer.TraCIPacket;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;

/**
 *  //todo comment
 */
public class ClientHandler implements Runnable{

	private static Logger logger = Logger.getLogger(ClientHandler.class);

	private final ServerSocket serverSocket;
	private final TraCISocket traCISocket;
	private final CommandExecutor cmdExecutor;
	private RemoteManager remoteManager;


	public ClientHandler(ServerSocket serverSocket, TraCISocket traCISocket) {
		this.serverSocket = serverSocket;
		this.traCISocket = traCISocket;
		this.remoteManager = new RemoteManager();
		this.cmdExecutor = new CommandExecutor(remoteManager);

	}


	@Override
	public void run() {
		try {
			handleClient();
		} catch (EOFException eof){
			logger.infof("EOF. Client closed socket");
		} catch (IOException e) {
			logger.error("Exception caught when trying to listen on port "
					+ 9999 + " or listening for a connection", e);
		}

	}

	private void handleClient() throws IOException{
		try{
			logger.info("client connected...");
			String filePath = "/home/stsc/repos/vadere/VadereManager/testResources/testProject001/scenarios/roVerTest001.scenario";

			String scenario = IOUtils.readTextFile(filePath);
			logger.infof("load File...");
			remoteManager.loadScenario(scenario);
			remoteManager.run();

			while (true){

				TraCIPacketBuffer traCIPacketBuffer = traCISocket.receiveExact();

				if (traCIPacketBuffer.hasRemaining()){
					TraCICommand cmd = traCIPacketBuffer.nextCommand();
					while (cmd != null ){


						TraCIPacket response = cmdExecutor.execute(cmd);
						logger.debugf("send packet with %d byte", response.size());
						traCISocket.sendExact(response);

						cmd = traCIPacketBuffer.nextCommand();
					}
				}

			}
		} catch (Exception e) {
			logger.error("Error while handling TraCI Message", e);
		} finally {
			traCISocket.close();
			remoteManager.stopSimulationIfRunning();
		}

	}

}
