package org.vadere.simulator.control;

import org.vadere.simulator.projects.Scenario;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.util.logging.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OfflineSimulation {

	private final Map<Step, List<Agent>> pedestriansByStep;
	private final Map<Integer, Trajectory> trajectories;
	private final Scenario vadere;
	private final List<SimulationState> simulationStates;
	private static Logger logger = Logger.getLogger(OfflineSimulation.class);
	private final Path outputDir;
	private final OfflineTopographyController topographyController;
	private final Topography topography;


	public OfflineSimulation(final Map<Step, List<Agent>> pedestriansByStep, final Scenario vadere,
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
				.map(id -> new Trajectory(pedestriansByStep, id, vadere.getAttributesSimulation().getSimTimeStepLength()))
				.collect(Collectors.toMap(t -> t.getPedestrianId(), t -> t));

		topographyController.prepareTopography();
		simulationStates = pedestriansByStep.keySet().stream().sorted().map(step -> generateSimulationState(step))
				.collect(Collectors.toList());
	}

	private SimulationState generateSimulationState(final Step step) {
		Topography topography = topographyController.getTopography().clone();
		topography.reset();
		// add pedestrians to the topography
		trajectories.values().stream()
				.filter(t -> t.isAlive(step))
				.map(t -> t.getAgent(step))
				.filter(opt -> opt.isPresent()).forEach(opt -> topography.addElement(opt.get()));
		return new SimulationState(vadere.getName(), topography, vadere.getScenarioStore(),
				(step.getStepNumber()-1) * vadere.getAttributesSimulation().getSimTimeStepLength(), step.getStepNumber(), null);
	}

	private void prepareOutput() {
	}

	public void run() {

		prepareOutput();

		if (!simulationStates.isEmpty()) {
			for (int index = 0; index < simulationStates.size(); index++) {
				SimulationState state = simulationStates.get(index);
				topographyController.update(state.getSimTimeInSec());
			}
		}
	}
}
