package org.vadere.simulator.control.simulation;

import org.vadere.simulator.control.scenarioelements.OfflineTopographyController;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.util.logging.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import tech.tablesaw.api.Table;

@Deprecated
public class OfflineSimulation {

	private final Table pedestriansByStep;
	private final Map<Integer, Trajectory> trajectories;
	private final Scenario vadere;
	private final List<SimulationState> simulationStates;
	private static Logger logger = Logger.getLogger(OfflineSimulation.class);
	private final Path outputDir;
	private final OfflineTopographyController topographyController;
	private final Topography topography;
	private final Random random;


	public OfflineSimulation(final Table pedestriansByStep, final Scenario vadere,
			final Path outputDir) {
		this.pedestriansByStep = pedestriansByStep;
		this.vadere = vadere;
		this.outputDir = outputDir;
		this.topography = vadere.getTopography();
		long seed = vadere.getAttributesSimulation().getSimulationSeed();
		this.random = new Random(seed);
		this.topographyController = new OfflineTopographyController(new Domain(topography), random);

		this.trajectories = null;
		this.simulationStates = null;
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
