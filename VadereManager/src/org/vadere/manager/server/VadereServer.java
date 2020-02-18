package org.vadere.manager.server;

import org.vadere.manager.ClientHandler;
import org.vadere.manager.TraCISocket;
import org.vadere.util.config.VadereConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class VadereServer extends AbstractVadereServer {

	private final ExecutorService handlerPool;

	public VadereServer(ServerSocket serverSocket, ExecutorService handlerPool, Path baseDir, boolean guiSupport, boolean trace) {
		super(serverSocket, baseDir, guiSupport, trace);
		this.handlerPool = handlerPool;
	}

	@Override
	public void run() {
		try {
			logger.infof("listening on port %d... (gui-mode: %s)", serverSocket.getLocalPort(), Boolean.toString(guiSupport));
			if (VadereConfig.getConfig().getBoolean("Vadere.cache.useGlobalCacheBaseDir")) {
				logger.infof("Cache location lookup searches at: %s",
						VadereConfig.getConfig().getString("Vadere.cache.globalCacheBaseDir"));
			}

			while (true) {
				Socket clientSocket = serverSocket.accept();
				handlerPool.execute(new ClientHandler(serverSocket, new TraCISocket(clientSocket, trace), baseDir, guiSupport));
			}
		} catch (IOException e) {
			e.printStackTrace();
			logger.warn("Interrupt Vadere Server");
		} finally {
			logger.info("Shutdown Vadere Server ...");
			handlerPool.shutdown();
			try {
				handlerPool.awaitTermination(4L, TimeUnit.SECONDS);
				if (!serverSocket.isClosed()) {
					serverSocket.close();
				}
			} catch (InterruptedException | IOException e) {
				logger.error(e);
			}
		}

	}
}
