package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesCrossingTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.util.Collection;

//TODO
@DataProcessorClass()
public class PedestrianCrossingTimeProcessor extends DataProcessor<PedestrianIdKey, Pair<Double, Double>>{

	private MeasurementArea measurementArea;
	private static Logger logger = Logger.getLogger(PedestrianCrossingTimeProcessor.class);

	public PedestrianCrossingTimeProcessor() {
		super("crossStartTime", "crossEndTime");
		setAttributes(new AttributesCrossingTimeProcessor());
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		for(Pedestrian ped : peds) {
			PedestrianIdKey key = new PedestrianIdKey(ped.getId());

			for(FootStep footStep : ped.getFootSteps()) {
				if(footStep.intersects(measurementArea.asVRectangle())) {

					double intersectionTime = footStep.computeIntersectionTime(measurementArea.asVRectangle());
					if(!hasCrossStartTime(key)) {
						setStartTime(key, intersectionTime);
					}
					else if(!hasCrossEndTime(key)) {
						setEndTime(key, intersectionTime);
					}
					else {
						assert false : "agent("+key.getPedestrianId()+") crosses the measurement area more than twice!";
						logger.error("agent("+key.getPedestrianId()+") crosses the measurement area more than twice!");
					}
				}
			}
		}
	}

	private void setStartTime(@NotNull final PedestrianIdKey key, double time) {
		putValue(key, Pair.of(time, Double.POSITIVE_INFINITY));
	}

	private void setEndTime(@NotNull final PedestrianIdKey key, double time) {
		putValue(key, Pair.of(getValue(key).getLeft(), time));
	}

	private boolean hasCrossStartTime(@NotNull final PedestrianIdKey key) {
		Pair<Double, Double> times = getValue(key);
		return times == null || times.getLeft().equals(Double.POSITIVE_INFINITY);
	}

	private boolean hasCrossEndTime(@NotNull final PedestrianIdKey key) {
		Pair<Double, Double> times = getValue(key);
		return hasCrossStartTime(key) && !times.getRight().equals(Double.POSITIVE_INFINITY);
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesCrossingTimeProcessor att = (AttributesCrossingTimeProcessor) this.getAttributes();
		this.measurementArea  = manager.getMeasurementArea(att.getMeasurementAreaId());
		if (measurementArea == null)
			throw new RuntimeException(String.format("MeasurementArea with index %d does not exist.", att.getMeasurementAreaId()));
		if (!measurementArea.isRectangular())
			throw new RuntimeException("DataProcessor and IntegralVoronoiAlgorithm only supports Rectangular measurement areas.");


	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesCrossingTimeProcessor());
		}
		return super.getAttributes();
	}

	@Override
	public String[] toStrings(@NotNull final  PedestrianIdKey key) {
		Pair<Double, Double> times = getValue(key);
		return new String[]{Double.toString(times.getLeft()), Double.toString(times.getRight())};
	}
}
