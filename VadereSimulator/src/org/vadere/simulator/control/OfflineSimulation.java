package org.vadere.simulator.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.dataprocessing.writer.ProcessorWriter;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.util.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OfflineSimulation {

	private final Map<Step, List<Agent>> pedestriansByStep;
	private final Map<Integer, Trajectory> trajectories;
	private final ScenarioRunManager vadere;
	private final List<SimulationState> simulationStates;
	private final List<ProcessorWriter> writers;
	private static Logger logger = LogManager.getLogger(OfflineSimulation.class);
	private final Path outputDir;
	private final OfflineTopographyController topographyController;
	private final Topography topography;


	public OfflineSimulation(final Map<Step, List<Agent>> pedestriansByStep, final ScenarioRunManager vadere,
			final Path outputDir) {
		this.pedestriansByStep = pedestriansByStep;
		this.vadere = vadere;
		this.outputDir = outputDir;
		this.topography = vadere.getTopography();
		this.topographyController = new OfflineTopographyController(topography);

		this.trajectories = pedestriansByStep
				.entrySet()
				.stream()
				.flatMap(entry -> entry.getValue().stream())
				.map(ped -> ped.getId())
				.distinct()
				.map(id -> new Trajectory(pedestriansByStep, id))
				.collect(Collectors.toMap(t -> t.getPedestrianId(), t -> t));

		topographyController.prepareTopography();
		simulationStates = pedestriansByStep.keySet().stream().sorted().map(step -> generateSimulationState(step))
				.collect(Collectors.toList());

		writers = vadere.getAllWriters();
	}

	private SimulationState generateSimulationState(final Step step) {
		Topography topography = topographyController.getTopography().clone();
		topography.reset();
		// add pedestrians to the topography
		trajectories.values().stream()
				.filter(t -> t.isPedestrianAlive(step))
				.map(t -> t.getAgent(step))
				.filter(opt -> opt.isPresent()).forEach(opt -> topography.addElement(opt.get()));
		return new SimulationState(vadere.getName(), topography, vadere.getScenarioStore(), vadere.getAllWriters(),
				step.getSimTimeInSec().orElse(Double.NaN), step.getStepNumber(), vadere.getProcessorManager());
	}

	private void prepareOutput() {
		DateFormat format = new SimpleDateFormat(IOUtils.DATE_FORMAT);
		int writerCounter = 0; // needed to distinguish writers with the same name
		String dateString = format.format(new Date());

		for (ProcessorWriter writer : writers) {
			Path processorOutputPath = outputDir;

			String filename = String.format("%s_%s_%d_%s%s", vadere.getName(), writer.getProcessor().getName(),
					(writerCounter++), dateString, writer.getProcessor().getFileExtension());
			String procFileName = IOUtils.getPath(processorOutputPath.toString(), filename).toAbsolutePath().toString();
			try {
				writer.setOutputStream(new FileOutputStream(procFileName, false));
			} catch (FileNotFoundException e) {
				logger.error(e);
			}
		}
	}

	public void run() {

		prepareOutput();

		if (!simulationStates.isEmpty()) {
			for (ProcessorWriter writer : writers) {
				writer.preLoop(simulationStates.get(0));
			}

			for (int index = 0; index < simulationStates.size(); index++) {
				SimulationState state = simulationStates.get(index);
				topographyController.update(state.getSimTimeInSec());
				for (ProcessorWriter writer : writers) {
					writer.update(state);
				}
			}

			for (ProcessorWriter writer : writers) {
				writer.postLoop(simulationStates.get(simulationStates.size() - 1));
			}
		}
	}
}
