package org.vadere.manager;

import de.tudresden.sumo.config.Constants;
import de.uniluebeck.itm.tcpip.Storage;

import it.polito.appeal.traci.protocol.Command;

import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.net.ServerSocket;

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
			Storage myInputStorage = new Storage();
			Storage myOutputStorage = new Storage();

			while (true){

				traCISocket.receiveExact(myInputStorage);
				Storage copy = copy(myInputStorage);
				Command cmd = new Command(copy);
				logger.infof("Recieved Command: 0x%02X", cmd.id());
				boolean status =false;
				if (cmd.id() == Constants.CMD_GETVERSION)
					status = cmdExecutor.execute(cmd.id(), myInputStorage, myOutputStorage);

				if (status){
					traCISocket.sendExact(myOutputStorage);
				}

				myInputStorage.reset();
				myOutputStorage.reset();

			}
		} catch (IOException e) {
			System.out.println("Exception caught when trying to listen on port "
					+ 9999 + " or listening for a connection");
			System.out.println(e.getMessage());
		}

	}
}
