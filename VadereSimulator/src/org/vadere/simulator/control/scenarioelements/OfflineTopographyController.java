package org.vadere.simulator.control.scenarioelements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.models.potential.fields.PotentialFieldDistancesBruteForce;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.mesh.EikonalSolverFMMTriangulation;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.IDistanceFunctionCached;
import org.vadere.util.random.SimpleReachablePointProvider;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class OfflineTopographyController {

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
	protected void prepareTopography() {
		// add boundaries
		if (domain.getTopography().isBounded() && !domain.getTopography().hasBoundary()) {
			for(Obstacle obstacle : Topography.createObstacleBoundary(domain.getTopography())) {
				domain.getTopography().addBoundary(obstacle);
			}
		}

		if(domain.getBackgroundMesh() != null) {
			IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = new IncrementalTriangulation<>(domain.getBackgroundMesh());
			List<PVertex> boundaryVertices = triangulation.getMesh().getBoundaryVertices();
			//IDistanceFunction distanceFunction = IDistanceFunction.create(new VRectangle(getTopography().getBounds()), getTopography().getObstacleShapes());
			EikonalSolverFMMTriangulation eikonalSolver = new EikonalSolverFMMTriangulation<>("distance", new UnitTimeCostFunction(), triangulation, boundaryVertices);
			eikonalSolver.initialize();

			//System.out.println(triangulation.getMesh().toPythonTriangulation(v -> triangulation.getMesh().getDoubleData(v, "distance_potential")));

			IDistanceFunctionCached cachedDistanceFunction = new IDistanceFunctionCached() {
				@Override
				public double apply(@NotNull IPoint point, Object caller) {
					return eikonalSolver.getPotential(point, caller);
				}

				@Override
				public Double apply(IPoint point) {
					return eikonalSolver.getPotential(point, null);
				}
			};

			//TODO use the cached caller
			//IDistanceFunction distanceFunction = p -> eikonalSolver.getPotential(p, null);
			getTopography().setObstacleDistanceFunction(cachedDistanceFunction);
			getTopography().setReachablePointProvider(SimpleReachablePointProvider.uniform(
					random,
					getTopography().getBounds(),
					iPoint -> eikonalSolver.getPotential(iPoint, null)));


		} else {
			// add distance function
			ScenarioCache cache = (ScenarioCache) VadereContext.get(getTopography()).getOrDefault("cache", ScenarioCache.empty());
			PotentialFieldDistancesBruteForce distanceField = new PotentialFieldDistancesBruteForce(
					getTopography().getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()),
					new VRectangle(getTopography().getBounds()),
					new AttributesFloorField(), cache);

			getTopography().setObstacleDistanceFunction(iPoint -> distanceField.getPotential(iPoint, null));

			// use PotentialFieldDistancesBruteForce as distance function
			getTopography().setReachablePointProvider(SimpleReachablePointProvider.uniform(
					random,
					getTopography().getBounds(),
					iPoint -> distanceField.getPotential(iPoint, null)));
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
