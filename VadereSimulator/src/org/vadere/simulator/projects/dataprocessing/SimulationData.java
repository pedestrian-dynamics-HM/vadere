package org.vadere.simulator.projects.dataprocessing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * {@link SimulationData} is a map between several {@link SimulationDataType}s
 * and corresponding List&lt;{@link TimeStep}&gt;s.
 * 
 */
public class SimulationData {
	private Map<SimulationDataType, List<TimeStep>> dataMap = new HashMap<SimulationDataType, List<TimeStep>>();

	public SimulationData() {}

	public void put(SimulationDataType key, List<TimeStep> data) {
		if (dataMap.containsKey(key)) {
			dataMap.get(key).addAll(data);
		} else {
			dataMap.put(key, data);
		}
	}

	public void put(SimulationDataType simulationDataType, TimeStep timeStep) {
		List<TimeStep> newList = new LinkedList<TimeStep>();
		newList.add(timeStep);
		put(simulationDataType, newList);
	}

	public List<TimeStep> get(SimulationDataType key) {
		return dataMap.get(key);
	}
}
