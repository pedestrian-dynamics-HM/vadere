package org.vadere.manager;

import org.vadere.manager.traci.TraCIVersion;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *  //todo comment
 */
public class VadereServer implements Runnable{

	private static Logger logger = Logger.getLogger(VadereServer.class);

	public static int SUPPORTED_TRACI_VERSION = 20;
//	public static int SUPPORTED_TRACI_VERSION = 1;
	public static String SUPPORTED_TRACI_VERSION_STRING = "Vadere Simulator. Supports subset of commands based von TraCI Version " + SUPPORTED_TRACI_VERSION;
	public static TraCIVersion currentVersion = TraCIVersion.V20_0_2;

	private final ServerSocket serverSocket;
	private final ExecutorService handlerPool;
	private final Path baseDir;
	private final boolean guiSupport;

	public VadereServer(ServerSocket serverSocket, ExecutorService handlerPool, Path baseDir, boolean guiSupport) {
		this.serverSocket = serverSocket;
		this.handlerPool = handlerPool;
		this.baseDir = baseDir;
		this.guiSupport = guiSupport;
	}

	@Override
	public void run() {
		try {
			logger.infof("listening on port %d... (gui-mode: %s)", serverSocket.getLocalPort(), Boolean.toString(guiSupport));
			if (VadereConfig.getConfig().getBoolean("Vadere.cache.useGlobalCacheBaseDir")){
				logger.infof("Cache location lookup searches at: %s",
						VadereConfig.getConfig().getString("Vadere.cache.globalCacheBaseDir"));
			}

			while (true) {
				Socket clientSocket = serverSocket.accept();

				handlerPool.execute(new ClientHandler(serverSocket, new TraCISocket(clientSocket), baseDir, guiSupport));
			}
		} catch (IOException e) {
			e.printStackTrace();
			logger.warn("Interrupt Vadere Server");
		} finally {
			logger.info("Shutdown Vadere Server ...");
			handlerPool.shutdown();
			try {
				handlerPool.awaitTermination(4L, TimeUnit.SECONDS);
				if (!serverSocket.isClosed()){
					serverSocket.close();
				}
			} catch (InterruptedException | IOException e) {
				logger.error(e);
			}
		}

	}
}
