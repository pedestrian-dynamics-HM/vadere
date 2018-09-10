package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.datakey.OverlapData;
import org.vadere.state.attributes.processor.AttributesMaxOverlapProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * This processor saves the largest overlap (2*pedRadius - distance between the pedestrian's centers) for one simulation.
 * The processor depends on the PedestrianOverlapDistProcessor. It only works if all pedestrians in the simulation have
 * the same radius.
 * 
 * @author Marion Gödel
 */


@DataProcessorClass()
public class MaxOverlapProcessor extends DataProcessor<NoDataKey, Double> {
	private PedestrianOverlapProcessor pedOverlapProc;


	public MaxOverlapProcessor() {
		super("max_size_overlap");
		setAttributes(new AttributesMaxOverlapProcessor());

	}

	@Override
	protected void doUpdate(final SimulationState state) {
		//ensure that all required DataProcessors are updated.
		this.pedOverlapProc.doUpdate(state);
	}

	@Override
	public void postLoop(final SimulationState state) {
		this.pedOverlapProc.postLoop(state);

		OverlapData maximumOverlap = this.pedOverlapProc.getData().values().stream().max(OverlapData::maxDist).orElse(OverlapData.noOverLap);
		this.putValue(NoDataKey.key(),maximumOverlap.getOverlap());
	}

	public void postLoopAddResultInfo(final SimulationState state, SimulationResult result){
		result.setMaxOverlap(this.getValue(NoDataKey.key()));

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
