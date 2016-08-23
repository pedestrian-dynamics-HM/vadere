package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Map;

public class PedestrianLastPositionProcessor extends Processor<PedestrianIdDataKey, VPoint> {
	private PedestrianPositionProcessor pedPosProc;

	public PedestrianLastPositionProcessor() {
		super("lastx" + LogFile.SEPARATOR +"lasty");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		this.pedPosProc.update(state);

		Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();
		pedPosMap.keySet().stream().forEach(pedId -> this.setValue(new PedestrianIdDataKey(pedId),
				this.pedPosProc.getValue(new TimestepPedestrianIdDataKey(state.getStep(), pedId))));
	}

	@Override
	void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		AttributesPedestrianLastPositionProcessor attLastPosProc =
				(AttributesPedestrianLastPositionProcessor) attributes;
		this.pedPosProc =
				(PedestrianPositionProcessor) manager.getProcessor(attLastPosProc.getPedestrianPositionProcessorId());
	}

	@Override
	public String toString(PedestrianIdDataKey key) {
		VPoint pos = this.getValue(key);

		return pos.x + LogFile.SEPARATOR.toString() + pos.y;
	}
}
