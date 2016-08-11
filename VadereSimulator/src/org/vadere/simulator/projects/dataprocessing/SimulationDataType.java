package org.vadere.simulator.projects.dataprocessing;

/**
 * A SimulationDataType is used by the classes implementing IOutputProcessor to
 * get simulation data from the simulation data store.
 * 
 */
public enum SimulationDataType {
	PEDESTRIAN_POSITION, SIMULATION_RUNTIME, PEDESTRIAN_SPEED, PEDESTRIAN_DENSITY, PEDESTRIAN_FLOW, PEDESTRIAN_EVACUATION_TIMES, PEDESTRIAN_EVACUATION_TIMESTAMPS, TOPOGRAPHY, ATTRIBUTES_MODEL, ATTRIBUTES_SIMULATION, ATTRIBUTES_PEDESTRIAN
}
