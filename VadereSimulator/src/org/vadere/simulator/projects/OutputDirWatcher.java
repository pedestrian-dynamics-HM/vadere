package org.vadere.simulator.projects;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements directory watcher to notify project if the filesystem changes.
 *
 * @author Stefan Schuhbäck
 */
public class OutputDirWatcher implements Runnable {
	private WatchService watchService;
	private ConcurrentHashMap<WatchKey, Path> keys;
	private List<WatchEventHandler> handlers;
	private VadereProject project;


	public OutputDirWatcher(VadereProject project) throws IOException {
		this.project = project;
	}


	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	private WatchEvent<Path>[] getWatchEvents(List<WatchEvent<?>> list) {
		List<WatchEvent<Path>> ret = new ArrayList<>();
		list.forEach(e -> ret.add(cast(e)));
		return list.toArray(new WatchEvent[list.size()]);
	}

	@Override
	public void run() {
		System.out.println("Start Watching...");
		try {
			while (true) {
				WatchKey key = watchService.take();
				Path dir = keys.get(key);
				if (dir == null) {
					System.out.println("Key not found: " + key.toString());
					continue;
				}

				WatchEvent<Path>[] events = getWatchEvents(key.pollEvents());

				handlers.forEach(handler -> handler.processEvent(dir, events));

				boolean valid = key.reset();
				if (!valid)
					keys.remove(key);

			}
		} catch (InterruptedException e) {
			try {
				watchService.close();
				System.out.println("Cleanup Watcher...");
			} catch (IOException e1) {
				//log
				e1.printStackTrace();
			}
			System.out.println("return from Watcher");
			return;
		}
	}

	public WatchService getWatchService() {
		return watchService;
	}

	public void setWatchService(WatchService watchService) {
		this.watchService = watchService;
	}

	public ConcurrentHashMap<WatchKey, Path> getKeys() {
		return keys;
	}

	public void setKeys(ConcurrentHashMap<WatchKey, Path> keys) {
		this.keys = keys;
	}

	public List<WatchEventHandler> getHandlers() {
		return handlers;
	}

	public void setHandlers(List<WatchEventHandler> handlers) {
		this.handlers = handlers;
	}

	public VadereProject getProject() {
		return project;
	}

	public void setProject(VadereProject project) {
		this.project = project;
	}
}
