package org.vadere.simulator.projects.dataprocessing;

import java.io.Closeable;

/**
 * Output result of simulation
 *
 * @author Stefan Schuhbäck
 */
public interface VadereWriter extends Closeable {

	void println(String s);

	void flush();
}
