package org.vadere.simulator.projects.dataprocessing.writer;

public class VadereStringWriterFactory extends VadereWriterFactory {

	public VadereStringWriterFactory() {

	}

	@Override
	public VadereWriter create(String path) {
		return new VadereStringWriter();
	}
}
