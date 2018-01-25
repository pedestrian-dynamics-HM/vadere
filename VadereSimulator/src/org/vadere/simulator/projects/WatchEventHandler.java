package org.vadere.simulator.projects;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 *
 * @author Stefan Schuhbäck
 */
@FunctionalInterface
public interface WatchEventHandler {
	void processEvent(Path dir, WatchEvent<Path>[] events);
}
