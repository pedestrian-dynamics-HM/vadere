package org.vadere.simulator.projects.dataprocessing.writer;

public abstract class VadereWriterFactory {

	public static VadereFileWriterFactory getFileWriterFactory() {
		return new VadereFileWriterFactory();
	}

	public static VadereStringWriterFactory getStringWriterFactory() {
		return new VadereStringWriterFactory();
	}

	public abstract VadereWriter create(String path);

}
