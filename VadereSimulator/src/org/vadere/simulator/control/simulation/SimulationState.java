package org.vadere.simulator.control.simulation;

import org.jetbrains.annotations.Nullable;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.scenario.Topography;

import java.util.Optional;

public class SimulationState {
	private final Topography topography;
	private final double simTimeInSec;
	private final ScenarioStore scenarioStore;
	private final int step;
	private final String name;
	private final MainModel mainModel;
	private final ControllerProvider controllerProvider;
	private boolean simStop = false;

	protected SimulationState(final String name,
							  final Topography topography,
							  final ScenarioStore scenarioStore,
							  final double simTimeInSec,
							  final int step,
							  @Nullable final MainModel mainModel,
							  final ControllerProvider controllerProvider) {
		this.name = name;
		this.topography = topography;
		this.simTimeInSec = simTimeInSec;
		this.step = step;
		this.scenarioStore = scenarioStore;
		this.mainModel = mainModel;
		this.controllerProvider = controllerProvider;
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

	public ControllerProvider getControllerProvider() {
		return controllerProvider;
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

	public  void setSimStop(boolean stop){
		if (!this.simStop)
			this.simStop = stop;
	}

	public boolean isSimStop() {
		return simStop;
	}


}
