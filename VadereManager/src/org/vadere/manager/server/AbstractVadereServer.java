package org.vadere.manager.server;

import org.vadere.manager.traci.TraCIVersion;
import org.vadere.util.logging.Logger;

import java.net.ServerSocket;
import java.nio.file.Path;

public abstract class AbstractVadereServer implements Runnable {
	protected static Logger logger = Logger.getLogger(VadereServer.class);

	public static int SUPPORTED_TRACI_VERSION = 20;
	//	public static int SUPPORTED_TRACI_VERSION = 1;
	public static String SUPPORTED_TRACI_VERSION_STRING = "Vadere Simulator. Supports subset of commands based von TraCI Version " + SUPPORTED_TRACI_VERSION;
	public static TraCIVersion currentVersion = TraCIVersion.V20_0_2;

	protected final ServerSocket serverSocket;
	protected final Path baseDir;
	protected final boolean guiSupport;

	public AbstractVadereServer(ServerSocket serverSocket, Path baseDir, boolean guiSupport) {
		this.serverSocket = serverSocket;
		this.baseDir = baseDir;
		this.guiSupport = guiSupport;
	}
}
