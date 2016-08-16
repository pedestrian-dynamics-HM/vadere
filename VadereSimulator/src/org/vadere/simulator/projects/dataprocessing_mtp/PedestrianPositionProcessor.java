package org.vadere.simulator.projects.dataprocessing_mtp;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;

public class PedestrianPositionProcessor extends Processor<TimestepPedestrianIdDataKey, VPoint> {

	public PedestrianPositionProcessor() {
		super("x y");
	}

	public Map<PedestrianIdDataKey, VPoint> getPositions(TimestepDataKey timestepKey) {
		return this.getColumn().entrySet().stream().filter(e -> e.getKey().getTimestep() == timestepKey.getKey())
				.collect(Collectors.toMap(e -> new PedestrianIdDataKey(e.getKey().getPedestrianId()), e -> e.getValue()));
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		Integer timeStep = state.getStep();
		Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();

		for (Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
			Integer pedId = entry.getKey();
			VPoint pos = entry.getValue();

			this.setValue(new TimestepPedestrianIdDataKey(timeStep, pedId), pos);
		}
	}

	@Override
	void init(final AttributesProcessor attributes, final ProcessorManager manager) {}

	@Override
	public String toString(TimestepPedestrianIdDataKey key) {
		VPoint p = this.getValue(key);

		return p.x + " " + p.y;
	}
}
