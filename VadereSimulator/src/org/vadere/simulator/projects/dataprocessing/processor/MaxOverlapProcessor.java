package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.datakey.OverlapData;
import org.vadere.state.attributes.processor.AttributesMaxOverlapProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

/**
 * This processor saves the largest overlap (2*pedRadius - distance between the pedestrian's centers) for one simulation.
 * The processor depends on the PedestrianOverlapDistProcessor. It only works if all pedestrians in the simulation have
 * the same radius.
 * 
 * @author Marion Gödel
 */


@DataProcessorClass()
public class MaxOverlapProcessor extends NoDataKeyProcessor<Double> {
	private PedestrianOverlapProcessor pedOverlapProc;


	public MaxOverlapProcessor() {
		super("max_size_overlap");
		setAttributes(new AttributesMaxOverlapProcessor());
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		//ensure that all required DataProcessors are updated.
		this.pedOverlapProc.update(state);
	}

	@Override
	public void postLoop(final SimulationState state) {
		this.pedOverlapProc.postLoop(state);

		OverlapData maximumOverlap = this.pedOverlapProc.getData().values().stream().max(OverlapData::maxDist).orElse(OverlapData.noOverLap);
		this.putValue(NoDataKey.key(),maximumOverlap.getOverlap());
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesMaxOverlapProcessor att = (AttributesMaxOverlapProcessor) this.getAttributes();
		this.pedOverlapProc = (PedestrianOverlapProcessor) manager.getProcessor(att.getPedestrianOverlapProcessorId());

	}


	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesMaxOverlapProcessor());
		}

		return super.getAttributes();
	}


}
