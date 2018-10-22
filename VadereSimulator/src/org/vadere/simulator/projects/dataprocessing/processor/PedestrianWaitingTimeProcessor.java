package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianWaitingTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VRectangle;

import java.util.Collection;

/**
 * @author Mario Teixeira Parente
 */
@DataProcessorClass()
public class PedestrianWaitingTimeProcessor extends DataProcessor<PedestrianIdKey, Double> {
	private double lastSimTime;
	private VRectangle waitingArea;

	public PedestrianWaitingTimeProcessor() {
		super("waitingTime");
		setAttributes(new AttributesPedestrianWaitingTimeProcessor());
		this.lastSimTime = 0.0;
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		double dt = state.getSimTimeInSec() - this.lastSimTime;

		for (Pedestrian p : peds) {
			int pedId = p.getId();
			VPoint pos = p.getPosition();

			if (this.waitingArea.contains(pos)) {
				PedestrianIdKey key = new PedestrianIdKey(pedId);
				this.putValue(key, (this.hasValue(key) ? this.getValue(key) : 0.0) + dt);
			}
		}

		this.lastSimTime = state.getSimTimeInSec();
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianWaitingTimeProcessor att = (AttributesPedestrianWaitingTimeProcessor) this.getAttributes();
		this.waitingArea = att.getWaitingArea();
		this.lastSimTime = 0.0;
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianWaitingTimeProcessor());
		}

		return super.getAttributes();
	}
}
