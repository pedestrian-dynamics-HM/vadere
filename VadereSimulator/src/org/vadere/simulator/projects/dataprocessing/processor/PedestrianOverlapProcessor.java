package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianOverlapProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;

/**
 * @author Mario Teixeira Parente
 */
@DataProcessorClass()
public class PedestrianOverlapProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer> {
	private double pedRadius;


	public PedestrianOverlapProcessor() {
		super("overlaps");
		setAttributes(new AttributesPedestrianOverlapProcessor());
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		this.pedRadius = state.getTopography().getAttributesPedestrian().getRadius();  // in init there is no access to the state
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
		peds.forEach(p -> this.putValue(
				new TimestepPedestrianIdKey(state.getStep(), p.getId()),
				this.calculateOverlaps(peds, p.getPosition())));
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianOverlapProcessor att = (AttributesPedestrianOverlapProcessor) this.getAttributes();

		this.pedRadius = att.getPedRadius();
	}

	private int calculateOverlaps(final Collection<Pedestrian> peds, VPoint pos) {
		return (int) peds.stream().filter(p -> p.getPosition().distance(pos) <= 2 * this.pedRadius).count() - 1;
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianOverlapProcessor());
		}

		return super.getAttributes();
	}
}
