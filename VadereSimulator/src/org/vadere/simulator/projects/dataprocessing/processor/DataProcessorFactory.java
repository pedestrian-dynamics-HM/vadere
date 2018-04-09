package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.store.DataProcessorStore;
import org.vadere.util.reflection.DynamicClassInstantiator;


public class DataProcessorFactory {

	private DynamicClassInstantiator<DataProcessor<?, ?>> processorInstantiator;

	public DataProcessorFactory() {
		processorInstantiator = new DynamicClassInstantiator<>();
	}

	public DataProcessor<?, ?> createDataProcessor(DataProcessorStore dataProcessorStore) {
		DataProcessor<?, ?> processor = processorInstantiator.createObject(dataProcessorStore.getType());
		processor.setId(dataProcessorStore.getId());
		processor.setAttributes(dataProcessorStore.getAttributes());
		return processor;
	}


	public DataProcessor<?, ?> createDataProcessor(String type) {
		DataProcessorStore dataProcessorStore = new DataProcessorStore();
		dataProcessorStore.setType(type);
		DataProcessor<?, ?> processor = processorInstantiator.createObject(dataProcessorStore.getType());
		processor.setId(dataProcessorStore.getId());
		processor.setAttributes(dataProcessorStore.getAttributes());
		return processor;
	}

	public DataProcessor<?, ?> createDataProcessor(Class type) {
		return createDataProcessor(type.getCanonicalName());
	}
}
