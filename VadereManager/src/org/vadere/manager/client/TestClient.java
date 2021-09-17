package org.vadere.manager.client;

import org.vadere.manager.TraCISocket;
import org.vadere.manager.traci.commands.control.TraCICloseCommand;
import org.vadere.manager.traci.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.traci.commands.control.TraCISendFileCommand;
import org.vadere.manager.traci.commands.control.TraCISimStepCommand;
import org.vadere.manager.traci.reader.TraCIPacketBuffer;
import org.vadere.manager.traci.response.TraCIGetResponse;
import org.vadere.manager.traci.response.TraCIResponse;
import org.vadere.manager.traci.response.TraCISimTimeResponse;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class TestClient extends org.vadere.manager.client.AbstractTestClient implements Runnable {

	private int port;
	private TraCISocket traCISocket;
	private ConsoleReader consoleReader;
	private Thread consoleThread;
	private String basePath;
	private String defaultScenario;
	private boolean running;

	public TestClient(int port, String basePath, String defaultScenario) {
		this.port = port;
		this.running = false;
		this.basePath = basePath;
		this.defaultScenario = defaultScenario;
	}


	public TestClient(int port, String[] args) {
		this.port = port;
		this.running = false;

		this.port = Integer.parseInt(args[0]);
		if (args.length == 3) {
			this.basePath = args[1];
			this.defaultScenario = args[2];
		} else if (args.length == 2) {
			this.basePath = args[1];
			this.defaultScenario = "";
		} else {
			this.basePath = "";
			this.defaultScenario = "";
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		TestClient testClient = new TestClient(9999, args);
		testClient.run();
	}

	private void addCommands(ConsoleReader consoleReader) {
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
		try {

			consoleReader = new ConsoleReader();
			addCommands(consoleReader);
			init(traCISocket, consoleReader);
			consoleThread = new Thread(consoleReader);
			if (!basePath.isEmpty() && !defaultScenario.isEmpty()) {
				System.out.println("send default file " + Paths.get(basePath, defaultScenario).toString());
				sendFile(new String[]{"send_file"});
			}
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

	void nextSimTimeStep(String[] args) throws IOException {
		double nextSimTime = -1.0;

		if (args.length > 1)
			nextSimTime = Double.parseDouble(args[1]);

		TraCIPacket packet = TraCISimStepCommand.build(nextSimTime);
		traCISocket.sendExact(packet);

		TraCISimTimeResponse cmd = (TraCISimTimeResponse) traCISocket.receiveResponse();
		System.out.println(cmd.toString());
	}

	void sendFile(String[] args) throws IOException {

		String filePath;

		if (args.length > 1) {
			if (!basePath.isEmpty()) {
				filePath = Paths.get(basePath, args[1] + ".scenario").toString();
			} else {
				filePath = args[1];
			}
		} else {
			if (!basePath.isEmpty() && !defaultScenario.isEmpty()) {
				filePath = Paths.get(basePath, defaultScenario).toString();
				System.out.println("use default " + defaultScenario);
			} else {
				System.out.println("no default scenario set");
				return;
			}
		}

		String data;
		try {
			data = IOUtils.readTextFile(filePath);
		} catch (IOException e) {
			System.out.println("File not found: " + filePath);
			return;
		}

		TraCIPacket packet = TraCISendFileCommand.TraCISendFileCommand("Test", data);

		traCISocket.sendExact(packet);

		TraCIPacketBuffer buf = traCISocket.receiveExact();
		TraCIResponse cmd = buf.nextResponse();

		System.out.println(cmd.toString());
	}

	private void printGet(TraCIResponse res) {
		if (res.isErr()) {
			System.out.println(res.toString());
		} else {
			System.out.println(res.getStatusResponse().toString());
			System.out.println("--> " + ((TraCIGetResponse) res).getResponseData());
		}
	}

	// personapi

	@Override
	public void personapi_getIdList(String[] args) throws IOException {
		TraCIResponse res = personapi.getIdList();
		printGet(res);
	}

	@Override
	public void personapi_getNextTargetListIndex(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument (id)");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = personapi.getNextTargetListIndex(elementID);
		printGet(res);
	}

	@Override
	public void personapi_setNextTargetListIndex(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("command needs argument id, nextTargetListIndex");
			return;
		}

		String elementIdentifier = args[1];
		int nextTargetListIndex = Integer.parseInt(args[2]);
		TraCIResponse res = personapi.setNextTargetListIndex(elementIdentifier, nextTargetListIndex);
		System.out.println(res.toString());
	}

	@Override
	public void personapi_getNextFreeId(String[] args) throws IOException {
		TraCIResponse res = personapi.getNextFreeId();
		printGet(res);
	}

	@Override
	public void personapi_getIdCount(String[] args) throws IOException {
		TraCIResponse res = personapi.getIdCount();
		printGet(res);
	}

	@Override
	public void personapi_getFreeFlowSpeed(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument (id)");
			return;
		}
		String elementIdentifier = args[1];

		try {
			TraCIGetResponse res = (TraCIGetResponse) personapi.getFreeFlowSpeed(elementIdentifier);
			double p = (double) res.getResponseData();
			System.out.println(p);
		} catch (ClassCastException e) {
			System.out.println("Maybe the id is invalid. See getIDList for valid ids.");
		}
	}

	@Override
	public void personapi_setFreeFlowSpeed(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("command needs argument id, velocity");
			return;
		}

		String elementIdentifier = args[1];
		double velocity = Double.parseDouble(args[2]);
		TraCIResponse res = personapi.setFreeFlowSpeed(elementIdentifier, velocity);
		System.out.println(res.toString());
	}

	@Override
	public void personapi_getPosition2D(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument (id)");
			return;
		}
		String elementIdentifier = args[1];
		TraCIGetResponse res = (TraCIGetResponse) personapi.getPosition2D(elementIdentifier);
		VPoint p = (VPoint) res.getResponseData();
		System.out.println(p.toString());
	}

	@Override
	public void personapi_setPosition2D(String[] args) throws IOException {
		if (args.length < 4) {
			System.out.println("command needs arguments id, x, y");
			return;
		}

		String elementIdentifier = args[1];
		double x = Double.parseDouble(args[2]);
		double y = Double.parseDouble(args[3]);
		VPoint p = new VPoint(x, y);
		TraCIResponse res = personapi.setPosition2D(elementIdentifier, p);
		System.out.println(res.toString());
	}

	@Override
	public void personapi_getPosition3D(String[] args) throws IOException {

	}

	@Override
	public void personapi_getVelocity(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument (id)");
			return;
		}
		String elementIdentifier = args[1];
		TraCIGetResponse res = (TraCIGetResponse) personapi.getVelocity(elementIdentifier);
		VPoint p = (VPoint) res.getResponseData();
		System.out.println(p.toString());
	}

	@Override
	public void personapi_getMaximumSpeed(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument (id)");
			return;
		}
		String elementIdentifier = args[1];
		TraCIGetResponse res = (TraCIGetResponse) personapi.getMaximumSpeed(elementIdentifier);
		double d = (Double) res.getResponseData();
		System.out.println(d);
	}

	@Override
	public void personapi_getPosition2DList(String[] args) throws IOException {
		TraCIResponse res = personapi.getPosition2DList();
		printGet(res);
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
		if (args.length < 2) {
			System.out.println("command needs argument (id)");
			return;
		}

		String elementIdentifier = args[1];
		TraCIGetResponse res = (TraCIGetResponse) personapi.getTargetList(elementIdentifier);
		ArrayList<String> targets = (ArrayList<String>) res.getResponseData();
		System.out.println(elementIdentifier + ": " + Arrays.toString(targets.toArray()));
	}

	@Override
	public void personapi_setTargetList(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("command needs argument element id and at least one target id");
			return;
		}

		String elementIdentifier = args[1];
		ArrayList<String> targets = new ArrayList<>(Arrays.asList(args).subList(2, args.length));

		TraCIResponse res = personapi.setTargetList(elementIdentifier, targets);
		System.out.println(res.toString());
	}

	@Override
	public void personapi_createNew(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument json file");
			return;
		}

		String dataPath = args[1];
		String data = "";
		try {
			data = IOUtils.readTextFile(dataPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		TraCIResponse res = personapi.createNew(data);
		System.out.println(res.toString());
	}

	@Override
	public void personapi_getHasNextTarget(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}

		String elementIdentifier = args[1];
		TraCIGetResponse res = (TraCIGetResponse) personapi.getHasNextTarget(elementIdentifier);
		System.out.println(res.getResponseData());
	}

	// simulationapi

	@Override
	public void simulationapi_getHash(String[] args) throws IOException {

		String data;
		try {
			data = IOUtils.readTextFile(Paths.get(basePath, defaultScenario).toString());
		} catch (IOException e) {
			System.out.println("File not found: " + Paths.get(basePath, defaultScenario).toString());
			return;
		}

		TraCIResponse cmd = simulationapi.getHash(data);

		System.out.println(cmd.toString());

	}

	@Override
	public void simulationapi_getDepartedPedestrianId(String[] args) throws IOException {

	}

	@Override
	public void simulationapi_getArrivedPedestrianIds(String[] args) throws IOException {

	}

	@Override
	public void simulationapi_getPositionConversion(String[] args) throws IOException {

	}

	@Override
	public void simulationapi_getCoordinateReference(String[] args) throws IOException {

	}

	@Override
	public void simulationapi_getOutputDir(final String[] args) throws IOException {

	}

	@java.lang.Override
	public void simulationapi_getObstacles(String[] args) throws IOException {

	}

	@Override
	public void simulationapi_getTime(String[] args) throws IOException {
		TraCIResponse res = simulationapi.getTime();
		System.out.println(res.toString());
	}

	@Override
	public void simulationapi_getTimeMs(String[] args) throws IOException {

	}

	@Override
	public void simulationapi_getSimSte(String[] args) throws IOException {

	}

	@Override
	public void simulationapi_init_control(final String[] args) throws IOException {

	}

	@Override
	public void simulationapi_apply_control(final String[] args) throws IOException {

	}

	@Override
	public void simulationapi_setSimConfig(String[] args) throws IOException {
		System.out.println("not implemented");
	}

	@Override
	public void simulationapi_getSimConfig(String[] args) throws IOException {

	}

	// polygonapi
	@Override
	public void polygonapi_getTopographyBounds(String[] args) throws IOException {
		TraCIResponse res = polygonapi.getTopographyBounds();
		System.out.println(res.toString());
	}

	@Override
	public void polygonapi_getIDList(String[] args) throws IOException {
		TraCIResponse res = polygonapi.getIDList();
		System.out.println(res.toString());
	}

	@Override
	public void polygonapi_getType(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = polygonapi.getType(elementID);
		System.out.println(res.toString());
	}

	@Override
	public void polygonapi_getShape(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = polygonapi.getShape(elementID);
		System.out.println(res.toString());
	}

	@Override
	public void polygonapi_getDistance(String[] args) throws IOException {
		if (args.length < 4) {
			System.out.println("command needs argument element id, x, y");
			return;
		}
		String elementID = args[1];
		ArrayList<String> point = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
		TraCIResponse res = polygonapi.getDistance(elementID, point);
		System.out.println(res.toString());
	}

	@Override
	public void polygonapi_getCentroid(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = polygonapi.getCentroid(elementID); // 0. is a dummy
		System.out.println(res.toString());
	}


	@Override
	public void polygonapi_getPosition2D(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = polygonapi.getPosition2D(elementID);
		System.out.println(res.toString());
	}


	@Override
	public void polygonapi_getIDCount(String[] args) throws IOException {
		TraCIResponse res = polygonapi.getIDCount();
		System.out.println(res.toString());
	}

	@Override
	public void polygonapi_getColor(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = polygonapi.getColor(elementID);
		System.out.println(res.toString());
	}


	@Override
	public void polygonapi_getImageFile(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = polygonapi.getImageFile(elementID);
		System.out.println(res.toString());
	}

	@Override
	public void polygonapi_getImageWidth(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = polygonapi.getImageWidth(elementID);
		System.out.println(res.toString());
	}

	@Override
	public void polygonapi_getImageHeight(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = polygonapi.getImageHeight(elementID);
		System.out.println(res.toString());
	}

	@Override
	public void polygonapi_getImageAngle(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}
		String elementID = args[1];
		TraCIResponse res = polygonapi.getImageAngle(elementID);
		System.out.println(res.toString());
	}

	@Override
	public void simulationapi_getDataProcessorValue(String[] args) throws IOException {

	}

	@Override
	public void simulationapi_getNetworkBound(String[] args) throws IOException {

	}

	// vadere api

	@Override
	public void miscapi_addStimulusInfos(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument json file");
		}

		String dataPath = args[1];
		String data = "";
		try {
			data = IOUtils.readTextFile(dataPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		TraCIResponse res = miscapi.addStimulusInfos(data);
		System.out.println(res.toString());
	}

	@Override
	public void miscapi_getAllStimulusInfos(String[] args) throws IOException {
		TraCIResponse res = miscapi.getAllStimulusInfos();
		System.out.println(res.toString());
	}

	@Override
	public void miscapi_createTargetChanger(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument json file");
			return;
		}

		String dataPath = args[1];
		String data = "";
		try {
			data = IOUtils.readTextFile(dataPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		TraCIResponse res = miscapi.createTargetChanger(data);
		System.out.println(res.toString());
	}

	@Override
	public void miscapi_removeTargetChanger(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("command needs argument element id");
			return;
		}

		String elementIdentifier = args[1];
		// todo the second parameter is a dummy
		TraCIResponse res = miscapi.removeTargetChanger(elementIdentifier);
		System.out.println(res.toString());
	}

	@Override
	public void personapi_setInformation(String[] args) throws IOException {

	}
}
