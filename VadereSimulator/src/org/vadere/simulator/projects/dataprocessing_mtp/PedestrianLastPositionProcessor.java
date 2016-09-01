package org.vadere.simulator.projects.dataprocessing_mtp;

import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;

public class PedestrianLastPositionProcessor extends Processor<PedestrianIdDataKey, VPoint> {
	private PedestrianPositionProcessor pedPosProc;

	public PedestrianLastPositionProcessor() {
		super("lastx", "lasty");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		this.pedPosProc.update(state);

		Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();
		pedPosMap.keySet().forEach(pedId -> this.addValue(new PedestrianIdDataKey(pedId),
				this.pedPosProc.getValue(new TimestepPedestrianIdDataKey(state.getStep(), pedId))));
	}

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		AttributesPedestrianLastPositionProcessor attLastPosProc =
				(AttributesPedestrianLastPositionProcessor) attributes;
		this.pedPosProc =
				(PedestrianPositionProcessor) manager.getProcessor(attLastPosProc.getPedestrianPositionProcessorId());
	}

	@Override
	public String[] toStrings(final PedestrianIdDataKey key) {
		VPoint pos = this.getValue(key);

		return new String[] { Double.toString(pos.x), Double.toString(pos.y) };
	}
}
