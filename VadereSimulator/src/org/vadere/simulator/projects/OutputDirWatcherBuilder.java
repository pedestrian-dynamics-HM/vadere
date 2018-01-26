package org.vadere.simulator.projects;

import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Builder class for {@link OutputDirWatcher}.
 *
 * @author Stefan Schuhbäck
 */
public class OutputDirWatcherBuilder {

	public static final WatchEvent.Kind<?>[] DEFAULT_KEYS =
			new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY};


	private OutputDirWatcher outputDirWatcher;

	private ConcurrentHashMap<WatchKey, Path> keys;
	private WatchService watchService;
	private List<WatchEventHandler> handlers;


	public OutputDirWatcherBuilder() {

	}

	public OutputDirWatcherBuilder initOutputDirWatcher(VadereProject project) throws IOException {
		this.outputDirWatcher = new OutputDirWatcher(project);

		this.watchService = FileSystems.getDefault().newWatchService();
		this.keys = new ConcurrentHashMap<>();
		this.handlers = new ArrayList<>();
		return this;
	}

	public OutputDirWatcherBuilder registerDefaultDir() throws IOException {
		Path root = outputDirWatcher.getProject().getOutputDir();
		registerAll(root);
		addDefaultEventHandler();
		return this;
	}

	public OutputDirWatcher build() {
		outputDirWatcher.setKeys(this.keys);
		outputDirWatcher.setHandlers(this.handlers);
		outputDirWatcher.setWatchService(this.watchService);
		return outputDirWatcher;
	}

	public void addDefaultEventHandler() {

		WatchEventHandler simulationOutputDirCreated = (dir, ev) -> {
			if ((ev.length == 1) && (ev[0].kind() == ENTRY_CREATE)) {
				Path context = dir.resolve(ev[0].context());
				if (context.toFile().isDirectory()) {
					System.out.println("simulationOutputDirCreated ...");
				}
			}
		};
		addEventHandler(simulationOutputDirCreated);

		WatchEventHandler simulationOutputDirModified = (dir, ev) -> {
			if ((ev.length == 2) && (ev[0].kind() == ENTRY_DELETE) && (ev[1].kind() == ENTRY_CREATE)) {
				Path context = dir.resolve(ev[1].context());
				if (context.toFile().isDirectory()) {
					System.out.print("simulationOutputDirModified new name: ");
					System.out.println(context.getFileName().toString());
				}
			}
		};
		addEventHandler(simulationOutputDirModified);

		WatchEventHandler simulationOutputFileNew = (dir, ev) -> {
			if ((ev.length == 2) && (ev[0].kind() == ENTRY_CREATE) && (ev[1].kind() == ENTRY_MODIFY)) {
				Path context = dir.resolve(ev[1].context());
				if (context.toFile().isFile()) {
					System.out.print("a file was created...");
					System.out.format(" in %s with the name of %s as dirty!%n", dir.getFileName().toString(), context.getFileName().toString());
				}
			}
		};
		addEventHandler(simulationOutputFileNew);

		WatchEventHandler simulationOutputFileModified = (dir, ev) -> {
			if ((ev.length == 1) && (ev[0].kind() == ENTRY_MODIFY)) {
				Path context = dir.resolve(ev[0].context());
				if (context.toFile().isFile()) {
					System.out.print("a file was modified...");
					System.out.format(" mark %s as dirty!%n", dir.getFileName().toString());
				}
			}
		};
		addEventHandler(simulationOutputFileModified);

		WatchEventHandler delete = (dir, ev) -> {
			if ((ev.length == 1) && (ev[0].kind() == ENTRY_DELETE)) {
				Path context = dir.resolve(ev[0].context());
				if (context.toFile().isFile()) {
					System.out.format("file deleted %s!%n", context.getFileName().toString());
				} else {
					System.out.format("dir deleted %s!%n", context.getFileName().toString());
				}
			}
		};
		addEventHandler(delete);

	}

	public OutputDirWatcherBuilder addEventHandler(WatchEventHandler handler) {
		this.handlers.add(handler);
		return this;
	}


	public void register(List<Path> dirs, WatchEvent.Kind<?>... events) throws IOException {
		for(Path dir : dirs){
			register(dir, events);
		}
	}

	public void register(Path dir, WatchEvent.Kind<?>... events) throws IOException {
		WatchEvent.Kind<?>[] selectedEvents;

		if (events.length <= 0) {
			selectedEvents = DEFAULT_KEYS;
		} else {
			selectedEvents = events;
		}

		if (dir.toString().contains(IOUtils.CORRUPT_DIR))
			return;

		WatchKey key = dir.register(watchService, selectedEvents);

		keys.put(key, dir);

	}

	public void registerAll(Path root, WatchEvent.Kind<?>... events) throws IOException {
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
					throws IOException {

				//do not watch corrupt directory or its children
				if (dir.endsWith(IOUtils.CORRUPT_DIR)) {
					return FileVisitResult.SKIP_SUBTREE;
				}

				register(dir, events);
				return FileVisitResult.CONTINUE;
			}

		});
	}

	public OutputDirWatcher getOutputDirWatcher() {
		return outputDirWatcher;
	}

	public void setOutputDirWatcher(OutputDirWatcher outputDirWatcher) {
		this.outputDirWatcher = outputDirWatcher;
	}

	public ConcurrentHashMap<WatchKey, Path> getKeys() {
		return keys;
	}

	public void setKeys(ConcurrentHashMap<WatchKey, Path> keys) {
		this.keys = keys;
	}

	public WatchService getWatchService() {
		return watchService;
	}

	public void setWatchService(WatchService watchService) {
		this.watchService = watchService;
	}

	public List<WatchEventHandler> getHandlers() {
		return handlers;
	}

	public void setHandlers(List<WatchEventHandler> handlers) {
		this.handlers = handlers;
	}
}
