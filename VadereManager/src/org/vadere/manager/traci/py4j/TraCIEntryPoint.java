/*
 * Author: Philipp Schuegraf
 */

package org.vadere.manager.traci.py4j;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.internal.HelpScreenException;

import org.vadere.manager.Manager;
import org.vadere.manager.TraCISocket;
import org.vadere.manager.client.traci.PersonAPI;
import org.vadere.manager.client.traci.PolygonAPI;
import org.vadere.manager.client.traci.SimulationAPI;
import org.vadere.manager.client.traci.MiscAPI;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import py4j.GatewayServer;

public class TraCIEntryPoint implements Runnable {

	static Logger logger;

	protected org.vadere.manager.client.traci.SimulationAPI simulationapi;
	protected org.vadere.manager.client.traci.PolygonAPI polygonapi;
	protected org.vadere.manager.client.traci.PersonAPI personapi;
	protected org.vadere.manager.client.traci.MiscAPI vadereapi;
	protected TraCIControl traciControl;

	private boolean running;
	private int port;
	private String bind;
	private int javaPort;
	private int pythonPort;
	private TraCISocket traCISocket;
	private String basePath = "";
	private String defaultScenario = "";

	TraCIEntryPoint(int port, String bind, int javaPort, int pythonPort) {
		this.port = port;
		this.bind = bind;
		this.javaPort = javaPort;
		this.pythonPort = pythonPort;
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		Logger.setMainArguments(args);
		logger = Logger.getLogger(Manager.class);
		ArgumentParser p = createArgumentParser();
		Namespace ns;

		try {
			ns = p.parseArgs(args);
//			ExecutorService pool = Executors.newFixedThreadPool(ns.getInt("clientNum"));
//			ServerSocket serverSocket = new ServerSocket(ns.getInt("port"));
//			logger.infof("Start Server(%s) with Loglevel: %s", VadereServer.currentVersion.getVersionString(), logger.getLevel().toString());
//			Thread serverThread = new Thread(new VadereServer(serverSocket, pool, Paths.get(ns.getString("output-dir")), ns.getBoolean("guiMode")));
//			serverThread.start();
			TraCIEntryPoint entryPoint = new TraCIEntryPoint(ns.getInt("port"), ns.getString("bind"), ns.getInt("javaPort"), ns.getInt("pythonPort"));
			entryPoint.basePath = ns.getString("basePath");
			entryPoint.defaultScenario = ns.getString("defaultScenario");
			entryPoint.run();
		} catch (HelpScreenException ignored) {

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static ArgumentParser createArgumentParser() {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("Vadere Server")
				.defaultHelp(true)
				.description("Runs the VADERE pedestrian simulator as a server for Python.");

		addOptionsToParser(parser);

		return parser;
	}

	private static void addOptionsToParser(ArgumentParser parser) {
		// no action required call to  Logger.setMainArguments(args) already configured Logger.
		parser.addArgument("--loglevel")
				.required(false)
				.type(String.class)
				.dest("loglevel")
				.choices("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL")
				.setDefault("INFO")
				.help("Set Log Level.");

		parser.addArgument("--logname")
				.required(false)
				.type(String.class)
				.dest("logname")
				.help("Write log to given file.");


		parser.addArgument("--server-port")
				.required(false)
				.type(Integer.class)
				.setDefault(9998)
				.dest("port")
				.help("Set port number.");


		parser.addArgument("--bind")
				.required(false)
				.type(String.class)
				.setDefault("127.0.0.1")
				.dest("bind")
				.help("Set ip number.");


		parser.addArgument("--java-port")
				.required(false)
				.type(Integer.class)
				.setDefault(10001)
				.dest("javaPort")
				.help("Set port number of gateway server for java.");


		parser.addArgument("--python-port")
				.required(false)
				.type(Integer.class)
				.setDefault(10002)
				.dest("pythonPort")
				.help("Set port number of gateway server for python.");

		parser.addArgument("--base-path")
				.required(true)
				.dest("basePath") // set name in namespace
				.type(String.class)
				.help("Supply base path as location for scenarios.");

		parser.addArgument("--default-scenario")
				.required(false)
				.setDefault("scenaio001")
				.dest("defaultScenario") // set name in namespace
				.type(String.class)
				.help("Supply default scenario.");
	}

	private void init() {
		simulationapi = new org.vadere.manager.client.traci.SimulationAPI(traCISocket);
		polygonapi = new org.vadere.manager.client.traci.PolygonAPI(traCISocket);
		personapi = new org.vadere.manager.client.traci.PersonAPI(traCISocket);
		vadereapi = new org.vadere.manager.client.traci.MiscAPI(traCISocket);
		traciControl = new TraCIControl(traCISocket, basePath, defaultScenario);
	}

	private void establishConnection() throws IOException, InterruptedException {
		Socket socket = new Socket();
		socket.setTcpNoDelay(true);
		int waitTime = 500; //ms
		System.out.println("Connect to " + this.bind + ":" + this.port);
		for (int i = 0; i < 14; i++) {
			try {
				socket.connect(new InetSocketAddress(this.bind, this.port));
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

	synchronized private void handleConnection() throws IOException, InterruptedException {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					logger.infof("close gateway");
					if (traCISocket != null) {
						try {
							traCISocket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				})
		);

		try {
			init();
			GatewayServer gatewayServer = new GatewayServer.GatewayServerBuilder(this)
					.javaPort(javaPort)
					.javaAddress(InetAddress.getByName(this.bind))
					.callbackClient(pythonPort, InetAddress.getByName(this.bind))
					.build();
			gatewayServer.start(false);
		} finally {
			if (traCISocket != null)
				traCISocket.close();
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

	public SimulationAPI getSimulationapi() {
		return simulationapi;
	}

	public PolygonAPI getPolygonapi() {
		return polygonapi;
	}

	public PersonAPI getPersonapi() {
		return personapi;
	}

	public MiscAPI getVadereapi() {
		return vadereapi;
	}

	public TraCIControl getTraciControl() {
		return traciControl;
	}

}