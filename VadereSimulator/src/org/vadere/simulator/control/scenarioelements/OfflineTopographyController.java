package org.vadere.simulator.control.scenarioelements;

import org.vadere.meshing.mesh.triangulation.DistanceFunctionApproxBF;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.models.potential.fields.PotentialFieldDistancesBruteForce;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.IDistanceFunctionCached;
import org.vadere.util.random.SimpleReachablePointProvider;

import java.util.Random;
import java.util.stream.Collectors;

public class OfflineTopographyController extends ScenarioElementController  {

	private final Domain domain;
	protected final Random random;

	public OfflineTopographyController(final Domain domain, final Random random) {
		this.domain = domain;
		this.random = random;
	}

	public void update(double simTimeInSec) {
		recomputeCells();
	}

	public Topography getTopography() {
		return domain.getTopography();
	}

	// add bounding box
	protected void prepareTopography(final AttributesFloorField attributesFloorField) {
		// add boundaries
		if (domain.getTopography().isBounded() && !domain.getTopography().hasBoundary()) {
			for(Obstacle obstacle : Topography.createObstacleBoundary(domain.getTopography())) {
				domain.getTopography().addBoundary(obstacle);
			}
		}

		if(domain.getBackgroundMesh() != null) {
			IDistanceFunction exactDistance = IDistanceFunction.create(new VRectangle(getTopography().getBounds()), getTopography().getObstacleShapes());
			IDistanceFunctionCached distanceFunction = new DistanceFunctionApproxBF(domain.getBackgroundMesh(), exactDistance);

			//TODO use the cached caller
			//IDistanceFunction distanceFunction = p -> eikonalSolver.getPotential(p, null);
			getTopography().setObstacleDistanceFunction(distanceFunction);
			getTopography().setReachablePointProvider(SimpleReachablePointProvider.uniform(
					random,
					getTopography().getBounds(),
					iPoint -> distanceFunction.apply(iPoint, null)));

		} else {
			// add distance function
			ScenarioCache cache = (ScenarioCache) VadereContext.getCtx(getTopography()).getOrDefault("cache", ScenarioCache.empty());
			PotentialFieldDistancesBruteForce distanceField = new PotentialFieldDistancesBruteForce(
					getTopography().getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()),
					new VRectangle(getTopography().getBounds()),
					attributesFloorField, cache);

			getTopography().setObstacleDistanceFunction(iPoint -> -distanceField.getPotential(iPoint, null));

			// use PotentialFieldDistancesBruteForce as distance function
			getTopography().setReachablePointProvider(SimpleReachablePointProvider.uniform(
					random,
					getTopography().getBounds(),
					iPoint -> -distanceField.getPotential(iPoint, null)));
		}
	}

	/**
	 * Recomputes the {@link org.vadere.util.geometry.LinkedCellsGrid} for fast access to pedestrian neighbors.
	 */
	protected void recomputeCells() {
		getTopography().getSpatialMap(Pedestrian.class).clear();
		for (Pedestrian pedestrian : getTopography().getElements(Pedestrian.class)) {
			getTopography().getSpatialMap(Pedestrian.class).addObject(pedestrian);
		}
		getTopography().getSpatialMap(Car.class).clear();
		for (Car car : getTopography().getElements(Car.class)) {
			getTopography().getSpatialMap(Car.class).addObject(car);
		}
		getTopography().setRecomputeCells(false);
	}
}
