package org.vadere.simulator.projects.dataprocessing_mtp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.reflection.DynamicClassInstantiator;

public class ProcessorFactory {
	private Map<Integer, Processor<?, ?>> processorMap;
	private Map<Integer, AttributesProcessor> attributesMap;

	public ProcessorFactory(List<Processor<?, ?>> processors, List<AttributesProcessor> attributesProcessor) {
		this.attributesMap = new HashMap<>();
		for (AttributesProcessor att : attributesProcessor)
			this.attributesMap.put(att.getProcessorId(), att);

		this.processorMap = new HashMap<>();
		for (Processor<?, ?> proc : processors)
			this.processorMap.put(proc.getId(), proc);

		processors.forEach(proc -> proc.init(this.attributesMap.get(proc.getId()), this));
	}

	public Processor<?, ?> getProcessor(int id) {
		return this.processorMap.containsKey(id) ? this.processorMap.get(id) : null;
	}

	public void updateProcessors(final SimulationState state) {
		this.processorMap.values().forEach(proc -> proc.update(state));
	}
}
