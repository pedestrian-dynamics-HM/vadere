package org.vadere.simulator.control.simulation;

import org.jetbrains.annotations.Nullable;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Map;
import java.util.Optional;

public class SimulationState {
	private final Topography topography;
	private final double simTimeInSec;
	private final ScenarioStore scenarioStore;
	private final int step;
	private final String name;
	private final MainModel mainModel;

	protected SimulationState(final String name,
							  final Topography topography,
							  final ScenarioStore scenarioStore,
							  final double simTimeInSec,
							  final int step,
							  @Nullable final MainModel mainModel) {
		this.name = name;
		this.topography = topography;
		this.simTimeInSec = simTimeInSec;
		this.step = step;
		this.scenarioStore = scenarioStore;
		this.mainModel = mainModel;

	}

	@Deprecated
	public SimulationState(final Map<Integer, VPoint> pedestrianPositionMap, final Topography topography,
						   final double simTimeInSec, final int step) {
		this.name = "";
		this.topography = topography;
		this.simTimeInSec = simTimeInSec;
		this.step = step;
		this.scenarioStore = null;
		this.mainModel = null;
	}

	// public access to getters

	public Topography getTopography() {
		return topography;
	}

	public double getSimTimeInSec() {
		return simTimeInSec;
	}

	public int getStep() {
		return step;
	}


	public ScenarioStore getScenarioStore() {
		return scenarioStore;
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns the main model of the simulation if it is an online simulation, otherwise the main
	 * model is unknown and therefore null.
	 *
	 * @return online simulation => main model, otherwise null.
	 */
	public Optional<MainModel> getMainModel() {
		return Optional.ofNullable(mainModel);
	}
}
