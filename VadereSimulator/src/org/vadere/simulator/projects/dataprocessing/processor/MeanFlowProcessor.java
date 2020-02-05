package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesMeanFlowProcessor;
import org.vadere.state.attributes.processor.AttributesMeanPedestrianEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.*;

/**
 * @author Marion Goedel
 * Evaluates the flow based on FundamentalDiagramAProcessor (zhang-2011)
 */

@DataProcessorClass()
public class MeanFlowProcessor extends NoDataKeyProcessor<Double> {
	private FundamentalDiagramAProcessor fundamentalDiagramAProcessor;
	private EvacuationTimeProcessor evacTimeProcessor;

	public MeanFlowProcessor() {
		super("flow");
		setAttributes(new AttributesMeanFlowProcessor());
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		//ensure that all required DataProcessors are updated.
		this.fundamentalDiagramAProcessor.update(state);
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesMeanFlowProcessor att = (AttributesMeanFlowProcessor) this.getAttributes();
		this.fundamentalDiagramAProcessor = (FundamentalDiagramAProcessor) manager.getProcessor(att.getPedestrianFundamentalDiagramAProcessorId());
		this.evacTimeProcessor = (EvacuationTimeProcessor) manager.getProcessor(att.getEvacuationTimeProcessorId());
	}

	@Override
	public void postLoop(final SimulationState state) {
		fundamentalDiagramAProcessor.postLoop(state);
		evacTimeProcessor.postLoop(state);

		double evacTime = evacTimeProcessor.getValue(NoDataKey.key());

		String[] headers = fundamentalDiagramAProcessor.getHeaders();
		int index_flow = Arrays.asList(headers).indexOf("flow");
		int index_time = Arrays.asList(headers).indexOf("measurementTime");
		Map flow = new HashMap<Double, Double>();
		// choose only values before evacuation time exceeds (to ignore zero entries after all pedestrians have left scenario)
		// todo: make sure that infinity only occurs at the end (why does it occur anyway?)
		fundamentalDiagramAProcessor.getValues().stream().filter(v-> v.get(index_time) < evacTime && Double.isInfinite(v.get(index_flow))).forEach(v->flow.put(v.get(index_time), v.get(index_flow)));
		// double mean = flow.values().stream().sum() / flow.size();
		double sumFlow = 0.0;
		for(int i = 1; i < flow.size(); i++){
			sumFlow += (double) flow.values().toArray()[i];
		}
		double meanFlow = sumFlow / flow.size();
		System.out.println();
		putValue(NoDataKey.key(), meanFlow);
		// putValue(NoDataKey.key(), evacTimes.parallelStream().reduce(0.0, (val1, val2) -> val1 + val2) / evacTimes.size());
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesMeanPedestrianEvacuationTimeProcessor());
		}

		return super.getAttributes();
	}
}
