package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

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
		Integer timeStep = state.getStep();

		VPoint offset = new VPoint(0,0);
		if (state.getName().contains("_with_offset")){
			offset = new VPoint(564280.000000, 5933391.000000);
		}

		for (Pedestrian p : state.getTopography().getElements(Pedestrian.class)) {
			this.putValue(new TimestepPedestrianIdKey(timeStep, p.getId()), getPosition(offset, p));
		}
	}

	private VPoint getPosition(VPoint offset, Pedestrian p){
		return p.getPosition().add(offset);
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
