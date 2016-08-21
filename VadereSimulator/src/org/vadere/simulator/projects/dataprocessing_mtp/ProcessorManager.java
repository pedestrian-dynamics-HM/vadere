package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.Model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProcessorManager {

	private Model model;
	private Map<Integer, Processor<?, ?>> processorMap;
	private Map<Integer, AttributesProcessor> attributesMap;

	private List<OutputFile<?>> writers;


	public ProcessorManager(List<Processor<?, ?>> processors, List<AttributesProcessor> attributesProcessor, List<OutputFile<?>> writers) {
		this(processors, attributesProcessor);
		this.writers = writers;
	}

	public ProcessorManager(List<Processor<?, ?>> processors, List<AttributesProcessor> attributesProcessor, Model model) {
		this(processors, attributesProcessor);
		this.model = model;
	}

	private ProcessorManager(List<Processor<?, ?>> processors, List<AttributesProcessor> attributesProcessor) {
		this.attributesMap = new LinkedHashMap<>();
		for (AttributesProcessor att : attributesProcessor)
			this.attributesMap.put(att.getProcessorId(), att);

		this.processorMap = new LinkedHashMap<>();
		for (Processor<?, ?> proc : processors)
			this.processorMap.put(proc.getId(), proc);

		processors.forEach(proc -> proc.init(this.attributesMap.get(proc.getId()), this));
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public void initWriters() {
		writers.forEach(writer -> writer.init(this));
	}

	public Processor<?, ?> getProcessor(int id) {
		return this.processorMap.containsKey(id) ? this.processorMap.get(id) : null;
	}

	public List<OutputFile<?>> getWriters() {
		return writers;
	}

	public Model getModel() {
		return this.model;
	}

	public void preLoop(final SimulationState state) {
		this.processorMap.values().forEach(proc -> proc.preLoop(state));
	}

	public void update(final SimulationState state) {
		this.processorMap.values().forEach(proc -> proc.update(state));
	}

	public void postLoop(final SimulationState state) {
		this.processorMap.values().forEach(proc -> proc.postLoop(state));
	}

	public Map<Integer, AttributesProcessor> getAttributesMap() {
		return attributesMap;
	}

	public Map<Integer, Processor<?, ?>> getProcessorMap() {
		return processorMap;
	}

}
