package org.vadere.simulator.projects.dataprocessing;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;

import java.io.File;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Teixeira Parente
 */

public class ProcessorManager {

	private MainModel mainModel;

	private Map<Integer, DataProcessor<?, ?>> processorMap;
	private List<OutputFile<?>> outputFiles;

	public ProcessorManager(List<DataProcessor<?, ?>> dataProcessors, List<OutputFile<?>> outputFiles, MainModel mainModel) {
		this.mainModel = mainModel;

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
		outputFiles.forEach(file -> file.init(processorMap));
	}

	public DataProcessor<?, ?> getProcessor(int id) {
		return this.processorMap.getOrDefault(id, null);
	}

	public MainModel getMainModel() {
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
		this.outputFiles.forEach(file -> file.setAbsoluteFileName(Paths.get(directory, String.format("%s", new File(file.getFileName()).getName())).toString()));
	}

	public void writeOutput() {
		this.outputFiles.forEach(file -> file.write());
	}


	/**
	 * Returns true if there is no output to write, otherwise false.
	 */
	public boolean isEmpty() {
		return processorMap.isEmpty();
	}

	public void sealAllAttributes() {
		processorMap.values().forEach(p -> p.sealAttributes());
	}
}
