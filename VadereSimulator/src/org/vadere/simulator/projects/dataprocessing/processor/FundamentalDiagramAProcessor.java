package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesFundamentalDiagramAProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>This processor computes the fundamental diagram by computing an (average) <tt>flow, density</tt> and
 * <tt>velocity</tt> over a certain time (<tt>deltaTime</tt>). This is done by counting the number of pedestrians crossing
 * a line for some <tt>deltaTime</tt>. If P is the set of pedestrians crossing in that duration
 * (<tt>deltaTime</tt>) the <tt>flow</tt> is defined by: the size of P divided by (te - ts) where
 * ts is the time the first pedestrian crossed the line and te is the time the last pedestrian crossed
 * the line i.e. (te - ts) is smaller or equals <tt>deltaTime</tt>. The <tt>velocity</tt> of a pedestrian
 * crossing the line is its velocity the time-step time in which the crossing happens. So it is not
 * exactly the crossing time (plus, minis simTimeStep). The <tt>density</tt> is defined by:
 * <tt>flow</tt> divided by (<tt>velocity</tt> times the length of the crossing line). Therefore, the
 * crossing line has to be defined appropriately. In addition the processor writes out <tt>deltaTime</tt>,
 * and the <tt>measurementTime</tt>. The first <tt>measurementTime</tt> is equal to <tt>deltaTime</tt> divided
 * by 2, the second is <tt>deltaTime</tt> plus <tt>deltaTime</tt> divided by 2 and so on.</p>
 *
 * <p>For more details see zhang-2011 (doi:10.1088/1742-5468/2011/06/P06004) Method A.</p>
 *
 * @author Benedikt Zoennchen
 *
 */
@DataProcessorClass()
public class FundamentalDiagramAProcessor extends DataProcessor<TimestepKey, List<Double>>  {

	private double deltaTime;
	private double deltaSimTime;
	private PedestrianLineCrossProcessor pedestrianLineCrossProcessor;
	private APedestrianVelocityProcessor pedestrianVelocityProcessor;

	public FundamentalDiagramAProcessor() {
		super("measurementTime", "deltaTime", "flow", "velocity", "density");
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesFundamentalDiagramAProcessor att = (AttributesFundamentalDiagramAProcessor) this.getAttributes();
		deltaTime = att.getDeltaTime();
		pedestrianLineCrossProcessor = (PedestrianLineCrossProcessor) manager.getProcessor(att.getPedestrianLineCrossProcessorId());
		pedestrianVelocityProcessor = (APedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesFundamentalDiagramAProcessor());
		}
		return super.getAttributes();
	}

	@Override
	public void preLoop(SimulationState state) {
		super.preLoop(state);
		deltaSimTime = -1;
	}

	@Override
	protected void doUpdate(SimulationState state) {
		if(deltaSimTime < 0 && state.getSimTimeInSec() > 0) {
			deltaSimTime = state.getSimTimeInSec();
		}

		pedestrianLineCrossProcessor.update(state);
		pedestrianVelocityProcessor.update(state);
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
