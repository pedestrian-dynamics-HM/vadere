package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianLastPositionProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;

/**
 * @author Mario Teixeira Parente
 */
@DataProcessorClass()
public class PedestrianLastPositionProcessor extends DataProcessor<PedestrianIdKey, VPoint> {
	private PedestrianPositionProcessor pedPosProc;

	public PedestrianLastPositionProcessor() {
		super("lastX", "lastY");
		setAttributes(new AttributesPedestrianLastPositionProcessor());
	}

	@Override
	public void doUpdate(final SimulationState state) {
		this.pedPosProc.update(state);

		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
		peds.stream().forEach(p -> {
			TimestepPedestrianIdKey key = new TimestepPedestrianIdKey(state.getStep(), p.getId());
			this.putValue(new PedestrianIdKey(p.getId()),
					this.pedPosProc.getValue(key));
		});
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianLastPositionProcessor attLastPosProc =
				(AttributesPedestrianLastPositionProcessor) this.getAttributes();
		this.pedPosProc =
				(PedestrianPositionProcessor) manager.getProcessor(attLastPosProc.getPedestrianPositionProcessorId());
	}

	@Override
	public String[] toStrings(final PedestrianIdKey key) {
		VPoint pos = this.getValue(key);

		return new String[]{Double.toString(pos.x), Double.toString(pos.y)};
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianLastPositionProcessor());
		}

		return super.getAttributes();
	}
}
