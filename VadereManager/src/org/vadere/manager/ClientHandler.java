package org.vadere.manager;

import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.TraCIPacketBuffer;
import org.vadere.util.logging.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;

public class ClientHandler implements Runnable{

	private static Logger logger = Logger.getLogger(ClientHandler.class);

	private final ServerSocket serverSocket;
	private final TraCISocket traCISocket;
	private final CommandExecutor cmdExecutor;
//	private final OutputQueue outputQueue;

	public ClientHandler(ServerSocket serverSocket, TraCISocket traCISocket) {
		this.serverSocket = serverSocket;
		this.traCISocket = traCISocket;
		this.cmdExecutor = new CommandExecutor();
//		this.outputQueue = new OutputQueue();
//		this.outputQueue.start();
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
			while (true){

				TraCIPacketBuffer traCIPacketBuffer = traCISocket.receiveExact();

				if (traCIPacketBuffer.hasRemaining()){
					TraCICommand cmd = traCIPacketBuffer.nextCommand();
					while (cmd != null ){


						TraCIPacket response = cmdExecutor.execute(cmd);
						logger.debugf("send packet with %d byte", response.size());
						traCISocket.sendExact(response);
//						outputQueue.put(response);

						cmd = traCIPacketBuffer.nextCommand();
					}
				}

			}
		} finally {
			traCISocket.close();
		}

	}

//	private class OutputQueue extends Thread{
//
//
//		private BlockingQueue<TraCIPacket> packets;
//		private boolean running;
//
//		OutputQueue(){
//			packets = new ArrayBlockingQueue<>(30);
//			running = true;
//		}
//
//		public void put(TraCIPacket packet){
//			try {
//				packets.put(packet);
//			} catch (InterruptedException e) {
//				cancel();
//				Thread.currentThread().interrupt();
//			}
//		}
//
//		public void cancel(){
//			running = false;
//		}
//
//		@Override
//		public void run(){
//			logger.info("Start output thread....");
//			try {
//				while (running){
//					TraCIPacket packet = packets.take();
//					System.out.println("send package");
//					traCISocket.sendExact(packet);
//
//					if (Thread.currentThread().isInterrupted())
//						running = false;
//				}
//			} catch (InterruptedException interEx) {
//				Thread.currentThread().interrupt();
//			} catch (IOException e) {
//				logger.error("Error sending TraciMessage",e);
//			} finally {
//				if (packets.size() > 0)
//					logger.warnf("Stop sending data with %d packet left in queue. Deleting packets...");
//
//			}
//		}
//	}
}
