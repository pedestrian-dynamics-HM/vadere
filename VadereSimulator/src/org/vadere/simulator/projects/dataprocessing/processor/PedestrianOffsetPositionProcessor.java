package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesOffsetPositionProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Mario Teixeira Parente
 */
@DataProcessorClass()
public class PedestrianOffsetPositionProcessor extends DataProcessor<TimestepPedestrianIdKey, VPoint> {

	public PedestrianOffsetPositionProcessor() {
		super("x_offset", "y_offset");
		setAttributes(new AttributesOffsetPositionProcessor());
	}

	Map<PedestrianIdKey, VPoint> getPositions(TimestepKey timestepKey) {
		return this.getData().entrySet().stream()
				.filter(e -> e.getKey().getTimestep().equals(timestepKey.getTimestep()))
				.collect(Collectors.toMap(e -> new PedestrianIdKey(e.getKey().getPedestrianId()), Map.Entry::getValue));
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		Integer timeStep = state.getStep();
		AttributesOffsetPositionProcessor attr = (AttributesOffsetPositionProcessor) getAttributes();
		for (Pedestrian p : state.getTopography().getElements(Pedestrian.class)) {
			this.putValue(new TimestepPedestrianIdKey(timeStep, p.getId()), p.getPosition().add(attr.getOffset()));
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
}
