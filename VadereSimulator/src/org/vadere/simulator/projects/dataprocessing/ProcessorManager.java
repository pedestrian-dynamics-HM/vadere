package org.vadere.simulator.projects.dataprocessing;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Topography;

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
	private final Topography topography;

	private Map<Integer, DataProcessor<?, ?>> processorMap;
	private List<OutputFile<?>> outputFiles;
	private SimulationResult simulationResult;

	public ProcessorManager(List<DataProcessor<?, ?>> dataProcessors,
							List<OutputFile<?>> outputFiles, MainModel mainModel,
							final Topography topography) {
		this.mainModel = mainModel;
		this.topography = topography;

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

	public MeasurementArea getMeasurementArea(int measurementAreaId){
		return topography.getMeasurementArea(measurementAreaId);
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
		this.processorMap.values().forEach(proc -> proc.postLoopAddResultInfo(state, simulationResult));
	}

	public void setOutputFiles(String directory) {
		// for each file
		this.outputFiles.forEach(file ->
				file.setAbsoluteFileName(
						Paths.get(directory, String.format("%s",
								new File(file.getFileName()).getName())).toString()));
	}

	public void writeOutput() {
		this.outputFiles.forEach(file -> file.write());
	}

	public void setSimulationResult(SimulationResult simulationResult) {
		this.simulationResult = simulationResult;
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