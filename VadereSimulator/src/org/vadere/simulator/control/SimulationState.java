package org.vadere.simulator.control;

import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.HashMap;
import java.util.Map;

public class SimulationState {
	private final Topography topography;
	private final Map<Integer, VPoint> pedestrianPositionMap;
	private final double simTimeInSec;
	private final ScenarioStore scenarioStore;
	private final int step;
	private final String name;
	private ProcessorManager processorManager;

	protected SimulationState(final String name,
							  final Topography topography,
							  final ScenarioStore scenarioStore,
							  final double simTimeInSec,
							  final int step,
							  final ProcessorManager processorManager) {
		this.name = name;
		this.topography = topography;
		this.simTimeInSec = simTimeInSec;
		this.step = step;
		this.pedestrianPositionMap = new HashMap<>();
		this.scenarioStore = scenarioStore;
		this.processorManager = processorManager;

		for (Pedestrian pedestrian : topography.getElements(Pedestrian.class)) {
			pedestrianPositionMap.put(pedestrian.getId(), pedestrian.getPosition());
		}
		for (Car car : topography.getElements(Car.class)) {
			pedestrianPositionMap.put(car.getId(), car.getPosition());
		}
	}

	@Deprecated
	public SimulationState(final Map<Integer, VPoint> pedestrianPositionMap, final Topography topography,
			final double simTimeInSec, final int step) {
		this.name = "";
		this.topography = topography;
		this.simTimeInSec = simTimeInSec;
		this.step = step;
		this.pedestrianPositionMap = pedestrianPositionMap;
		this.scenarioStore = null;
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

	public Map<Integer, VPoint> getPedestrianPositionMap() {
		return pedestrianPositionMap;
	}

	public ScenarioStore getScenarioStore() {
		return scenarioStore;
	}

	public String getName() {
		return name;
	}

	public ProcessorManager getProcessorManager() {
		return this.processorManager;
	}
}
