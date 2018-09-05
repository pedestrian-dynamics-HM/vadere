package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.datakey.OverlapData;
import org.vadere.state.attributes.processor.AttributesNumberOverlapsProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Optional;

/**
 * This processor counts the number of overlaps during a simulation run.
 * The is commented code that can be used to show a info box if overlaps occured.
 * 
 * @author Marion Gödel
 */


@DataProcessorClass()
public class NumberOverlapsProcessor extends DataProcessor<NoDataKey, Long> {
	private PedestrianOverlapProcessor pedOverlapProc;


	public NumberOverlapsProcessor() {
		super("nr_overlaps");
		setAttributes(new AttributesNumberOverlapsProcessor());
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		//ensure that all required DataProcessors are updated.
		this.pedOverlapProc.doUpdate(state);
	}

	@Override
	public void postLoop(final SimulationState state) {
		this.pedOverlapProc.postLoop(state);

		long numberOverlaps = this.pedOverlapProc.getData().size();
		this.putValue(NoDataKey.key(), numberOverlaps);
	}

	@Override
	public void postLoopAddResultInfo(final SimulationState state, SimulationResult result){
		result.setTotalOverlaps(this.pedOverlapProc.getData().size());
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesNumberOverlapsProcessor att = (AttributesNumberOverlapsProcessor) this.getAttributes();
		this.pedOverlapProc = (PedestrianOverlapProcessor) manager.getProcessor(att.getPedestrianOverlapProcessorId());

	}


	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesNumberOverlapsProcessor());
		}

		return super.getAttributes();
	}


}
