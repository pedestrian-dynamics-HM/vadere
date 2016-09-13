package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdDataKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdDataKey;
import org.vadere.state.attributes.processor.AttributesPedestrianLastPositionProcessor;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Map;

public class PedestrianLastPositionProcessor extends DataProcessor<PedestrianIdDataKey, VPoint> {
	private PedestrianPositionProcessor pedPosProc;

	public PedestrianLastPositionProcessor() {
		super("lastx", "lasty");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		this.pedPosProc.update(state);

		Map<Integer, VPoint> pedPosMap = state.getPedestrianPositionMap();
		pedPosMap.keySet().forEach(pedId -> this.addValue(new PedestrianIdDataKey(pedId),
				this.pedPosProc.getValue(new TimestepPedestrianIdDataKey(state.getStep(), pedId))));
	}

	@Override
	public void init(final ProcessorManager manager) {
		AttributesPedestrianLastPositionProcessor attLastPosProc =
				(AttributesPedestrianLastPositionProcessor) this.getAttributes();
		this.pedPosProc =
				(PedestrianPositionProcessor) manager.getProcessor(attLastPosProc.getPedestrianPositionProcessorId());
	}

	@Override
	public String[] toStrings(final PedestrianIdDataKey key) {
		VPoint pos = this.getValue(key);

		return new String[] { Double.toString(pos.x), Double.toString(pos.y) };
	}
}
