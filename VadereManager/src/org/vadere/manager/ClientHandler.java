package org.vadere.manager;

import de.uniluebeck.itm.tcpip.Storage;

import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayDeque;
import java.util.Queue;

public class ClientHandler implements Runnable{

	private static Logger logger = Logger.getLogger(ClientHandler.class);

	private final ServerSocket serverSocket;
	private final TraCISocket traCISocket;
	private final CommandExecutor cmdExecutor;

	public ClientHandler(ServerSocket serverSocket, TraCISocket traCISocket) {
		this.serverSocket = serverSocket;
		this.traCISocket = traCISocket;
		this.cmdExecutor = new CommandExecutor();
	}

	Storage copy(Storage s){
		byte[] data = new byte[s.size() - s.position()];
		for(int i=s.position(); i<s.getStorageList().size(); i++){
			data[i] = s.getStorageList().get(i);
		}
		return new Storage(data);
	}

	@Override
	public void run() {

		try{
			Queue<TraciCommand> receivedCommands = new ArrayDeque<>();
			TraCiMessageBuilder msgBuilder = new TraCiMessageBuilder();

			while (true){

				traCISocket.receiveExact(receivedCommands);

				for(TraciCommand cmd: receivedCommands){
					logger.infof("Recieved Command: 0x%02X", cmd.getId());
					boolean status = cmdExecutor.execute(cmd, msgBuilder);
					if (status){
						traCISocket.send(msgBuilder.build());
					}
				}

			}
		} catch (IOException e) {
			System.out.println("Exception caught when trying to listen on port "
					+ 9999 + " or listening for a connection");
			System.out.println(e.getMessage());
		}

	}
}
