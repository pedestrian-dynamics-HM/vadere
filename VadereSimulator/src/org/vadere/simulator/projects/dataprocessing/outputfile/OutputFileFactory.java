package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;
import org.vadere.util.reflection.DynamicClassInstantiator;

public class OutputFileFactory {
	private final DynamicClassInstantiator<OutputFile<?>> outputFileInstantiator;

	public OutputFileFactory() {
		outputFileInstantiator = new DynamicClassInstantiator<>();
	}

	public OutputFile<?> createOutputfile(OutputFileStore fileStore) {
		OutputFile<?> file = outputFileInstantiator.createObject(fileStore.getType());
		file.setRelativeFileName(fileStore.getFilename());
		file.setProcessorIds(fileStore.getProcessors());
		file.setSeparator(fileStore.getSeparator());
		return file;
	}

	public OutputFile<?> createDefaultOutputfile() {
		OutputFileStore fileStore = new OutputFileStore();
		OutputFile<?> file = outputFileInstantiator.createObject(fileStore.getType());
		file.setSeparator(fileStore.getSeparator());
		return file;
	}

	public OutputFile<?> createOutputfile(String type) {
		OutputFileStore fileStore = new OutputFileStore();
		fileStore.setType(type);
		OutputFile<?> file = outputFileInstantiator.createObject(fileStore.getType());
		file.setSeparator(fileStore.getSeparator());
		return file;
	}

	public OutputFile<?> createOutputfile(Class type) {
		return createOutputfile(type.getCanonicalName());
	}

}
