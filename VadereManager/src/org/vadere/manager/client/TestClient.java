package org.vadere.manager.client;

import org.vadere.manager.TraCISocket;
import org.vadere.manager.traci.commands.control.TraCICloseCommand;
import org.vadere.manager.traci.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.traci.commands.control.TraCISendFileCommand;
import org.vadere.manager.traci.commands.control.TraCISimStepCommand;
import org.vadere.manager.traci.reader.TraCIPacketBuffer;
import org.vadere.manager.traci.respons.TraCIGetResponse;
import org.vadere.manager.traci.respons.TraCIResponse;
import org.vadere.manager.traci.respons.TraCISimTimeResponse;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class TestClient extends org.vadere.manager.client.AbstractTestClient implements Runnable{

	private int port;
	private TraCISocket traCISocket;
	private ConsoleReader consoleReader;
	private Thread consoleThread;
	private boolean running;

	public static void main(String[] args) throws IOException, InterruptedException {
		TestClient testClient = new TestClient(9999);
		testClient.run();
	}


	public TestClient(int port) {
		this.port = port;
		this.running = false;
	}


	private void addCommands(ConsoleReader consoleReader){
		consoleReader.addCommand("ctr.getVersion", "", this::getVersion);
		consoleReader.addCommand("ctr.sendFile", "send file. default: scenario001", this::sendFile);
		consoleReader.addCommand("ctr.nextStep", "default(-1) one loop.", this::nextSimTimeStep);
		consoleReader.addCommand("ctr.close", "Close application and stop running simulations", this::close);
	}

	private void establishConnection() throws IOException, InterruptedException {
			Socket socket = new Socket();
			socket.setTcpNoDelay(true);
			int waitTime = 500; //ms
			System.out.println("Connect to 127.0.0.1:" + this.port);
			for (int i = 0; i < 14; i++) {
				try {
					socket.connect(new InetSocketAddress("127.0.0.1", this.port));
					break;
				} catch (ConnectException ex) {
					Thread.sleep(waitTime);
					waitTime *= 2;
				}
			}

			if (!socket.isConnected()) {
				System.out.println("can't connect to Server!");
				return;
			}

			System.out.println("connected...");
			traCISocket = new TraCISocket(socket);

			running = true;
	}

	private void handleConnection() throws IOException {
		try{

			consoleReader = new ConsoleReader();
			addCommands(consoleReader);
			init(traCISocket, consoleReader);
			consoleThread = new Thread(consoleReader);
			consoleThread.start();

			consoleThread.join();


		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (traCISocket != null)
				traCISocket.close();
			if (consoleReader != null)
				consoleReader.stop();
		}
	}


	@Override
	public void run() {

		try {
			establishConnection();
			handleConnection();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	// Commands

	void getVersion(String[] args) throws IOException {
		TraCIPacket p = TraCIGetVersionCommand.build();
		traCISocket.sendExact(p);

		TraCIPacketBuffer buf = traCISocket.receiveExact();
		TraCIResponse cmd = buf.nextResponse();

		System.out.println(cmd.toString());

	}

	void close(String[] args) throws IOException {

		traCISocket.sendExact(TraCICloseCommand.build());

		TraCIResponse cmd = traCISocket.receiveResponse();
		System.out.println(cmd);

		System.out.println("Bye");
		consoleReader.stop();
	}


	void nextSimTimeStep(String[] args) throws IOException{
		double nextSimTime = -1.0;

		if (args.length > 1)
			nextSimTime = Double.parseDouble(args[1]);

		TraCIPacket packet = TraCISimStepCommand.build(nextSimTime);
		traCISocket.sendExact(packet);

		TraCISimTimeResponse cmd = (TraCISimTimeResponse) traCISocket.receiveResponse();
		System.out.println(cmd.toString());
	}


	void sendFile(String[] args) throws IOException {

		String filePath = "/Users/Philipp/IdeaProjects/vadere/VadereManager/testResources/testProject001/scenarios/";

		if (args.length > 1) {
			filePath = filePath + args[1] + ".scenario";
		} else {
			System.out.println("use default scenario001.scenario");
			filePath = filePath + "scenario001.scenario";
		}

		String data;
		try{
			data = IOUtils.readTextFile(filePath);
		} catch (IOException e){
			System.out.println("File not found: " + filePath);
			return;
		}

		TraCIPacket packet = TraCISendFileCommand.TraCISendFileCommand("Test", data);

		traCISocket.sendExact(packet);

		TraCIPacketBuffer buf = traCISocket.receiveExact();
		TraCIResponse cmd = buf.nextResponse();

		System.out.println(cmd.toString());
	}

	@Override
	public void personapi_getIDList(String[] args) throws IOException {
		TraCIGetResponse res = personapi.getIDList();
		System.out.println(res.getResponseData());
	}

	@Override
	public void personapi_getIDCount(String[] args) throws IOException {

	}

	@Override
	public void personapi_getSpeed(String[] args) throws IOException {

	}

	@Override
	public void personapi_getPosition2D(String[] args) throws IOException {
		if(args.length < 2){
			System.out.println("command needs argument (id)");
			return;
		}
		String elementIdentifier = args[1];
		TraCIGetResponse res = personapi.getPosition2D(elementIdentifier);
		VPoint p = (VPoint) res.getResponseData();
		System.out.println(p.toString());
	}

	@Override
	public void personapi_getPosition3D(String[] args) throws IOException {

	}

	@Override
	public void personapi_getLength(String[] args) throws IOException {

	}

	@Override
	public void personapi_getWidth(String[] args) throws IOException {

	}

	@Override
	public void personapi_getRoadId(String[] args) throws IOException {

	}

	@Override
	public void personapi_getAngle(String[] args) throws IOException {

	}

	@Override
	public void personapi_getType(String[] args) throws IOException {

	}

	@Override
	public void personapi_getTargetList(String[] args) throws IOException {
		if(args.length < 2){
			System.out.println("command needs argument (id)");
			return;
		}

		String elementIdentifier = args[1];
		TraCIGetResponse res = personapi.getTargetList(elementIdentifier);
		ArrayList<String> targets = (ArrayList<String>) res.getResponseData();
		System.out.println(elementIdentifier + ": " + Arrays.toString(targets.toArray()));
	}

	@Override
	public void personapi_setTargetList(String[] args) throws IOException {
		if(args.length < 3){
			System.out.println("command needs argument element id and at least one target id");
			return;
		}

		String elementIdentifier = args[1];
		ArrayList<String> targets = new ArrayList<>();
		for (int i = 2; i < args.length; i++){
			targets.add(args[i]);
		}

		TraCIResponse res =  personapi.setTargetList(elementIdentifier, targets);
		System.out.println(res.toString());
	}
}
