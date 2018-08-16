package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesMaxOverlapProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.*;
import java.util.stream.Stream;

/**
 * This processor gives for each pedestrian - if at least one overlap occurs in this time step - the overlap with
 * the closest other pedestrian. That is 2*radius - distance (between the centers of the pedestrians). That means, if
 * several overlaps occur at the same timestep, only the largest one is shown.
 * At the moment, the radius needs to be equal for all pedestrians in the simulation.
 *
 * @author Marion Gödel
 */

@DataProcessorClass()
public class PedestrianOverlapDistProcessor extends DataProcessor<TimestepPedestrianIdKey, Double> {


	public PedestrianOverlapDistProcessor() {
		super("overlap_dist");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		double pedRadius = state.getTopography().getAttributesPedestrian().getRadius();  // in init there is no access to the state
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
		peds.forEach(p -> this.putValue(
				new TimestepPedestrianIdKey(state.getStep(), p.getId()),
				this.calculateOverlaps(peds, p, pedRadius)));
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	private double calculateOverlaps(final Collection<Pedestrian> peds, Pedestrian ped, double pedRadius) {
		VPoint pos = ped.getPosition();
		Stream<Pedestrian> pedList = peds.stream().filter(p -> (!p.equals(ped)) ? p.getPosition().distance(pos) <= 2 * pedRadius: false);
		List<Double> overlaps = new ArrayList<Double>();
		pedList.forEach(p -> overlaps.add(2*pedRadius - p.getPosition().distance(pos)));
		double overlapDist = (overlaps.isEmpty()) ? 0 : Collections.max(overlaps);
		return overlapDist;
	}

}
