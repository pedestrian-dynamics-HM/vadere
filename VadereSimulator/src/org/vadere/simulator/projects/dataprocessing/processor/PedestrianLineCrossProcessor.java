package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianLineCrossProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VLine;

import java.util.Collection;


/**
 * This processor computes the exact time a pedestrian crossed a line (last).
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class PedestrianLineCrossProcessor extends DataProcessor<PedestrianIdKey, Double> {

	private VLine line;

	public PedestrianLineCrossProcessor() {
		super("crossTime");
		setAttributes(new AttributesPedestrianLineCrossProcessor());
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		for(Pedestrian ped : peds) {
			PedestrianIdKey key = new PedestrianIdKey(ped.getId());

			for(FootStep footStep : ped.getTrajectory()) {
				if(footStep.intersects(line)) {
					double intersectionTime = footStep.computeIntersectionTime(line);
					this.putValue(key, intersectionTime);
				}
			}
		}
	}

	public VLine getLine() {
		return line;
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianLineCrossProcessor att = (AttributesPedestrianLineCrossProcessor) this.getAttributes();
		this.line = new VLine(att.getP1(), att.getP2());
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianLineCrossProcessor());
		}
		return super.getAttributes();
	}
}
