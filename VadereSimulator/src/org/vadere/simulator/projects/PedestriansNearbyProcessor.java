package org.vadere.simulator.projects;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestriansNearbyData;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestriansNearbyIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Maxim Dudin
 */

@DataProcessorClass()
public class PedestriansNearbyProcessor extends DataProcessor<TimestepPedestriansNearbyIdKey, PedestriansNearbyData> {
	private double maxDistance = 1.5; // todo adjustable with json


	public PedestriansNearbyProcessor() {
		super("durationTimesteps");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
		int timeStep = state.getStep();
		for (Pedestrian ped : peds) {
			// get all Pedestrians with at most maxDistance away
			// this reduces the amount of overlap tests
			VPoint pedPos = ped.getPosition();
			List<DynamicElement> dynElemNneighbours = getDynElementsAtPosition(state.getTopography(), ped.getPosition(), maxDistance);
			List<PedestriansNearbyData> pedsNearby = dynElemNneighbours
					.parallelStream()
					.filter(p -> ped.getId() != p.getId())
					.map(p -> new PedestriansNearbyData(ped.getId(), p.getId(), 1, timeStep))
					.collect(Collectors.toList());
			pedsNearby.forEach(o -> this.putValue(new TimestepPedestriansNearbyIdKey(timeStep, o.getPedId1(), o.getPedId2()), o));
		}
	}

	public String[] toStrings(final  TimestepPedestriansNearbyIdKey key) {
		return  this.hasValue(key) ? this.getValue(key).toStrings() : new String[]{"N/A", "N/A"};
	}

	@Override
	protected void putValue(final TimestepPedestriansNearbyIdKey key, final PedestriansNearbyData value) {
		for (TimestepPedestriansNearbyIdKey alreadyExisting : getKeys()) {
			PedestriansNearbyData currentVal = getValue(alreadyExisting);
			if (key.isAccountedForBy(currentVal)) {
				return;
			} else if (key.isContinuationOf(currentVal)) {
				super.putValue(alreadyExisting, currentVal.getDataWithIncrementedDuration());
				return;
			}
		}
		super.putValue(key, value);
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	private List<DynamicElement> getDynElementsAtPosition(final Topography topography, VPoint sourcePosition, double radius) {
		LinkedCellsGrid<DynamicElement> dynElements = topography.getSpatialMap(DynamicElement.class);
		return dynElements.getObjects(sourcePosition, radius);
	}

}
