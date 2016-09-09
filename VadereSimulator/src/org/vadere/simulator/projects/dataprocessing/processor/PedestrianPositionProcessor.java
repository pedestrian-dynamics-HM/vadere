package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdDataKey;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepDataKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdDataKey;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class PedestrianPositionProcessor extends Processor<TimestepPedestrianIdDataKey, VPoint> {

	public PedestrianPositionProcessor() {
		super("x", "y");
	}

	public Map<PedestrianIdDataKey, VPoint> getPositions(TimestepDataKey timestepKey) {
		return this.getData().entrySet().stream()
				.filter(e -> e.getKey().equals(timestepKey))
				.collect(Collectors.toMap(e -> new PedestrianIdDataKey(e.getKey().getPedestrianId()), e -> e.getValue()));
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		Integer timeStep = state.getStep();
		Map<Integer, VPoint> pedPosMap = state.getPedestrianPositionMap();

		for (Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
			Integer pedId = entry.getKey();
			VPoint pos = entry.getValue();

			this.addValue(new TimestepPedestrianIdDataKey(timeStep, pedId), pos);
		}
	}

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {}

	@Override
	public String[] toStrings(TimestepPedestrianIdDataKey key) {
		VPoint p = this.getValue(key);

		return new String[] { Double.toString(p.x), Double.toString(p.y) };
	}
}
