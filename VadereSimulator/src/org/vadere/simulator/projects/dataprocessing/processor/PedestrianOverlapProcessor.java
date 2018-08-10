package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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
	private static Logger logger = LogManager.getLogger(PedestrianOverlapProcessor.class);
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
		long overlap = peds.stream().filter(p -> p.getPosition().distance(pos) <= 2 * this.pedRadius).count() - 1;
		return (int)overlap;
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianOverlapProcessor());
		}

		return super.getAttributes();
	}
}
