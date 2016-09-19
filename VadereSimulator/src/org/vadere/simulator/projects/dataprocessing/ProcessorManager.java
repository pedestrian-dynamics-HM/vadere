package org.vadere.simulator.projects.dataprocessing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.util.io.IOUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProcessorManager {
	private DataProcessingJsonManager jsonManager;

	private MainModel mainModel;

	private Map<Integer, DataProcessor<?, ?>> processorMap;
	private List<OutputFile<?>> outputFiles;

	public ProcessorManager(DataProcessingJsonManager jsonManager, List<DataProcessor<?, ?>> dataProcessors, List<OutputFile<?>> outputFiles) {
		this.jsonManager = jsonManager;

		this.outputFiles = outputFiles;

		this.processorMap = new LinkedHashMap<>();
		for (DataProcessor<?, ?> proc : dataProcessors)
			this.processorMap.put(proc.getId(), proc);

		dataProcessors.forEach(proc -> proc.init(this));
	}

	public void setMainModel(MainModel mainModel) {
		this.mainModel = mainModel;
	}

	public void initOutputFiles() {
		outputFiles.forEach(file -> file.init(this));
	}

	public DataProcessor<?, ?> getProcessor(int id) {
		return this.processorMap.containsKey(id) ? this.processorMap.get(id) : null;
	}

	public MainModel getMainModel() {
		if (mainModel == null)
			throw new IllegalStateException(
					"The main model is not available until the simulation has started (Simulation.run())");
		return mainModel;
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

	public void setOutputPath(String directory) {
		this.outputFiles.forEach(file -> file.setFileName(IOUtils.getPath(directory, String.format("%s", file.getFileName())).toString()));
	}

	public void writeOutput() {
        this.outputFiles.forEach(file -> file.write());
    }

    public JsonNode serializeToNode() throws JsonProcessingException {
    	return this.jsonManager.serializeToNode();
	}
}
