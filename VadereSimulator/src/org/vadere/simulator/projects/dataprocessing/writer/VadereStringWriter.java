package org.vadere.simulator.projects.dataprocessing.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VadereStringWriter implements VadereWriter {

	List<String> output;

	public VadereStringWriter() {
		output = new ArrayList<>();
	}

	public List<String> getOutput() {
		return output;
	}

	@Override
	public void println(String s) {
		output.add(s);
	}

	@Override
	public void flush() {

	}

	@Override
	public void close() throws IOException {

	}
}
