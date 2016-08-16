package org.vadere.simulator.projects.dataprocessing_mtp;

import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;

public class PedestrianLastPositionProcessor extends Processor<PedestrianIdDataKey, VPoint> {
	private PedestrianPositionProcessor pedPosProc;

	public PedestrianLastPositionProcessor() {
		super("lastx lasty");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		this.pedPosProc.update(state);

		Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();
		pedPosMap.keySet().stream().forEach(pedId -> this.setValue(new PedestrianIdDataKey(pedId),
				this.pedPosProc.getValue(new TimestepPedestrianIdDataKey(state.getStep(), pedId))));
	}

	@Override
	void init(final AttributesProcessor attributes, final ProcessorManager factory) {
		AttributesPedestrianLastPositionProcessor attLastPosProc =
				(AttributesPedestrianLastPositionProcessor) attributes;
		this.pedPosProc =
				(PedestrianPositionProcessor) factory.getProcessor(attLastPosProc.getPedestrianPositionProcessorId());
	}

	@Override
	public String toString(PedestrianIdDataKey key) {
		VPoint pos = this.getValue(key);

		return pos.x + " " + pos.y;
	}
}
