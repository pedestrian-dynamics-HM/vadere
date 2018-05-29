package org.vadere.simulator.projects.dataprocessing.writer;

public class VadereFileWriterFactory extends VadereWriterFactory {

	public VadereFileWriterFactory() {
	}

	@Override
	public VadereWriter create(String path) {
		return new VadereFileWriter(path);
	}
}
