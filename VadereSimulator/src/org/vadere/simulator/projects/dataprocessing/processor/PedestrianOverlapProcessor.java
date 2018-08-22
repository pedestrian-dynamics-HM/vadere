package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdOverlap;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mario Teixeira Parente
 */
@DataProcessorClass()
public class PedestrianOverlapProcessor extends DataProcessor<TimestepPedestrianIdOverlap, Double> {
	private double minDist;


	public PedestrianOverlapProcessor() {
		super("overlaps");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		double pedRadius = state.getTopography().getAttributesPedestrian().getRadius();
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
		minDist = pedRadius * 2;
		int timeStep = state.getStep();
		for (Pedestrian ped : peds) {
			// get all Pedestrians with at moust pedRadius*2.5 distance away
			// this reduces the amount auf overlap test.
			VPoint pedPos = ped.getPosition();
			List<DynamicElement> neighbours = getDynElementsAtPosition(state.getTopography(), ped.getPosition(), pedRadius *2.5);
			// collect pedIds and distance of all overlaps for the current ped in the current timestep
			List<Pair> overlaps = neighbours
					.parallelStream()
					.map(p -> new Pair(p.getId(), p.getPosition().distance(pedPos)))
					.filter(p -> ped.getId() != p.id)
					.filter(Pair::isOverlap)
					.collect(Collectors.toList());
			overlaps.forEach(o -> this.putValue(new TimestepPedestrianIdOverlap(timeStep, ped.getId(), o.id), o.dist));
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	private List<DynamicElement> getDynElementsAtPosition(final Topography topography, VPoint sourcePosition, double radius) {
		LinkedCellsGrid<DynamicElement> dynElements = topography.getSpatialMap(DynamicElement.class);
		return dynElements.getObjects(sourcePosition, radius);
	}


	class Pair{
		int id;
		double dist;

		public Pair(int id, double dist) {
			this.id = id;
			this.dist = dist;
		}

		boolean isOverlap(){
			return dist < minDist;
		}
	}
}
