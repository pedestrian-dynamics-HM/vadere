package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author Mario Teixeira Parente
 */

public class PedestrianPositionProcessor extends DataProcessor<TimestepPedestrianIdKey, VPoint> {

	public PedestrianPositionProcessor() {
		super("x", "y");
	}

	public Map<PedestrianIdKey, VPoint> getPositions(TimestepKey timestepKey) {
		return this.getData().entrySet().stream()
				.filter(e -> e.getKey().equals(timestepKey))
				.collect(Collectors.toMap(e -> new PedestrianIdKey(e.getKey().getPedestrianId()), e -> e.getValue()));
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		Integer timeStep = state.getStep();
		Map<Integer, VPoint> pedPosMap = state.getPedestrianPositionMap();

		for (Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
			Integer pedId = entry.getKey();
			VPoint pos = entry.getValue();

			this.putValue(new TimestepPedestrianIdKey(timeStep, pedId), pos);
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	@Override
	public String[] toStrings(TimestepPedestrianIdKey key) {
		VPoint p = this.getValue(key);

		return new String[]{Double.toString(p.x), Double.toString(p.y)};
	}
}
