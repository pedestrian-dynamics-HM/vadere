package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianLastPositionProcessor;
import org.vadere.state.attributes.processor.AttributesPedestrianPositionProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Mario Teixeira Parente
 */
@DataProcessorClass()
public class PedestrianPositionProcessor extends DataProcessor<TimestepPedestrianIdKey, VPoint> {

	public PedestrianPositionProcessor() {
		super("x", "y");
	}

	Map<PedestrianIdKey, VPoint> getPositions(TimestepKey timestepKey) {
		return this.getData().entrySet().stream()
				.filter(e -> e.getKey().getTimestep().equals(timestepKey.getTimestep()))
				.collect(Collectors.toMap(e -> new PedestrianIdKey(e.getKey().getPedestrianId()), Map.Entry::getValue));
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		// This does not work currently, bcause of the mocking in the tests.
		// Collection<Pedestrian> pedestrians = state.getTopography().getPedestrianDynamicElements().getElements();
		Collection<Pedestrian> pedestrians = state.getTopography().getElements(Pedestrian.class);
		Integer timeStep = state.getStep();
		double simTime = state.getSimTimeInSec();

		for (Pedestrian pedestrian : pedestrians){
			VPoint position = getAttributes().isInterpolate() ? pedestrian.getInterpolatedFootStepPosition(simTime) : pedestrian.getPosition();
			this.putValue(new TimestepPedestrianIdKey(timeStep, pedestrian.getId()), position);
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	@Override
	public String[] toStrings(TimestepPedestrianIdKey key) {
		VPoint p = this.getValue(key);
		if(p == null) {
			return new String[]{Double.toString(0), Double.toString(0)};
		}
		else {
			return new String[]{Double.toString(p.x), Double.toString(p.y)};
		}
		//return new String[]{Double.toString(p.x), Double.toString(p.y)};
	}

	@Override
	public AttributesPedestrianPositionProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianPositionProcessor());
		}
		return (AttributesPedestrianPositionProcessor)super.getAttributes();
	}
}
