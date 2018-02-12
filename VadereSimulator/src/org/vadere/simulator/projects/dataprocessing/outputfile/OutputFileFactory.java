package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;
import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;
import org.vadere.util.reflection.DynamicClassInstantiator;
import org.vadere.util.reflection.VadereNoOutputfileForDataKeyException;

import java.util.Arrays;

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

	public OutputFile<?> createOutputfile(Class type, Integer... processorsIds) {
		OutputFile<?> file = createOutputfile(type.getCanonicalName());
		file.setProcessorIds(Arrays.asList(processorsIds));
		return file;
	}

	public OutputFile<?> createDefaultOutputfileByDataKey(Class<? extends DataKey<?>> keyType, Integer... processorsIds) {
		OutputFile<?> file = createDefaultOutputfileByDataKey(keyType);
		file.setProcessorIds(Arrays.asList(processorsIds));
		return file;
	}

	public OutputFile<?> createDefaultOutputfileByDataKey(Class<? extends DataKey<?>> keyType) {

		String simpleName = keyType.getSimpleName();

		if (simpleName.equals("IdDataKey"))
			return createOutputfile(IdOutputFile.class);

		if (simpleName.equals("NoDataKey"))
			return createOutputfile(NoDataKeyOutputFile.class);

		if (simpleName.equals("PedestrianIdKey"))
			return createOutputfile(PedestrianIdOutputFile.class);

		if (simpleName.equals("TimestepKey"))
			return createOutputfile(TimestepOutputFile.class);

		if (simpleName.equals("TimestepPedestrianIdKey"))
			return createOutputfile(TimestepPedestrianIdOutputFile.class);

		if (simpleName.equals("TimestepPositionKey"))
			return createOutputfile(TimestepPositionOutputFile.class);

		if (simpleName.equals("TimestepRowKey"))
			return createOutputfile(TimestepRowOutputFile.class);


		throw new VadereNoOutputfileForDataKeyException(
				"No Ouputfile defined for DataKey: " + keyType.getCanonicalName());
	}
}
