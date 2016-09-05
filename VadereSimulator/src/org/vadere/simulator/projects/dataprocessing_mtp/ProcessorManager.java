package org.vadere.simulator.projects.dataprocessing_mtp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.Model;
import org.vadere.util.io.IOUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProcessorManager {
	private DataProcessingJsonManager jsonManager;

	private Model model;

	private Map<Integer, Processor<?, ?>> processorMap;
	private Map<Integer, AttributesProcessor> attributesMap;
	private List<OutputFile<?>> outputFiles;

	public ProcessorManager(DataProcessingJsonManager jsonManager, List<Processor<?, ?>> processors, List<AttributesProcessor> attributesProcessor, List<OutputFile<?>> outputFiles) {
		this.jsonManager = jsonManager;

		this.outputFiles = outputFiles;

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

	public void initOutputFiles() {
		outputFiles.forEach(file -> file.init(this));
	}

	public Processor<?, ?> getProcessor(int id) {
		return this.processorMap.containsKey(id) ? this.processorMap.get(id) : null;
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

	public AttributesProcessor getAttributes(int processorId) {
		return this.attributesMap.get(processorId);
	}

	public void setOutputPath(String directory) {
		String dateString = new SimpleDateFormat(IOUtils.DATE_FORMAT).format(new Date());
		this.outputFiles.forEach(file -> file.setFileName(IOUtils.getPath(directory, String.format("%s_%s", dateString, file.getFileName())).toString()));
	}

	public void writeOutput() {
        this.outputFiles.forEach(file -> file.write());
    }

    public JsonNode serializeToNode() throws JsonProcessingException {
    	return this.jsonManager.serializeToNode();
	}
}
