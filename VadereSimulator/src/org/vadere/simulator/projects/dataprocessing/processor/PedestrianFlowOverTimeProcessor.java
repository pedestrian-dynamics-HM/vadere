package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesFlowOverTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * see zhang-2011 Method A.
 */
@DataProcessorClass()
public class PedestrianFlowOverTimeProcessor extends DataProcessor<TimestepKey, List<Double>>  {

	private double deltaTime;
	private double deltaSimTime;
	private PedestrianLineCrossProcessor pedestrianLineCrossProcessor;
	private PedestrianVelocityProcessor pedestrianVelocityProcessor;

	public PedestrianFlowOverTimeProcessor() {
		super("time", "deltaTime", "flow", "velocity", "density");
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesFlowOverTimeProcessor att = (AttributesFlowOverTimeProcessor) this.getAttributes();
		deltaTime = att.getDeltaTime();
		deltaSimTime = -1;
		pedestrianLineCrossProcessor = (PedestrianLineCrossProcessor) manager.getProcessor(att.getPedestrianLineCrossProcessorId());
		pedestrianVelocityProcessor = (PedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesFlowOverTimeProcessor());
		}
		return super.getAttributes();
	}

	@Override
	protected void doUpdate(SimulationState state) {
		if(deltaSimTime < 0 && state.getSimTimeInSec() > 0) {
			deltaSimTime = state.getSimTimeInSec();
		}

		pedestrianLineCrossProcessor.doUpdate(state);
		pedestrianVelocityProcessor.doUpdate(state);
		double simTime = state.getSimTimeInSec();
		double measureTime = simTime - deltaTime / 2.0;

		if(state.getSimTimeInSec() >= deltaTime) {
			final double localStartTime = simTime - deltaTime;
			final double localEndTime = simTime;

			Map<PedestrianIdKey, Double> crossingTimes = pedestrianLineCrossProcessor.getData();
			int N = 0;
			double velocity = 0.0;
			double minTime = Double.MAX_VALUE;
			double maxTime = Double.MIN_VALUE;

			for(Map.Entry<PedestrianIdKey, Double> entry : crossingTimes.entrySet()) {
				double crossingTime = entry.getValue();

				int crossingStep = 1;
				if(deltaSimTime > 0) {
					crossingStep = (int)Math.floor(crossingTime / deltaSimTime);
				}

				if(crossingTime >= localStartTime && crossingTime < localEndTime) {
					N++;
					velocity += pedestrianVelocityProcessor.getValue(new TimestepPedestrianIdKey(crossingStep, entry.getKey().getPedestrianId()));
					minTime = Math.min(minTime, crossingTime);
					maxTime = Math.max(maxTime, crossingTime);
				}
			}

			double flow = 0.0;
			double density = 0.0;

			if(N > 0) {
				flow = N / (maxTime - minTime);
				velocity /= N;
				density = flow / (velocity * pedestrianLineCrossProcessor.getLine().length());
			}

			putValue(new TimestepKey(state.getStep()), Arrays.asList(measureTime, deltaTime, flow, velocity, density));
		}
	}

	@Override
	public String[] toStrings(TimestepKey key) {
		List<Double> data = this.getValue(key);
		if(data == null) {
			return new String[]{
					Double.toString(Double.NaN),
					Double.toString(Double.NaN),
					Double.toString(Double.NaN),
					Double.toString(Double.NaN),
					Double.toString(Double.NaN)
			};
		}
		else {
			return new String[]{
					Double.toString(data.get(0)),
					Double.toString(data.get(1)),
					Double.toString(data.get(2)),
					Double.toString(data.get(3)),
					Double.toString(data.get(4))
			};
		}
	}
}
