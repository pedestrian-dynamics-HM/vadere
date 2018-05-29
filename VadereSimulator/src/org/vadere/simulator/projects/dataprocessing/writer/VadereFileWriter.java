package org.vadere.simulator.projects.dataprocessing.writer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

/**
 * Output result of simulation as file to the filesystem.
 *
 * @author Stefan Schuhbäck
 */
public class VadereFileWriter implements VadereWriter {

	PrintWriter w;

	public VadereFileWriter(String absoluteFileName) {
		try {
			this.w = new PrintWriter(new FileWriter(absoluteFileName));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void println(String s) {
		w.println(s);
	}

	@Override
	public void flush() {
		w.flush();
	}


	@Override
	public void close() throws IOException {
		w.close();
	}
}
