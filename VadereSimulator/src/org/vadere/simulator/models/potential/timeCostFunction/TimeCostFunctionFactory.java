package org.vadere.simulator.models.potential.timeCostFunction;

import org.apache.commons.math3.special.Erf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.utils.math.GeometryUtilsMesh;
import org.vadere.simulator.models.density.IGaussianFilter;
import org.vadere.simulator.models.potential.solver.calculators.mesh.DensityUpdater;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunctionMesh;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.simulator.models.queuing.QueueingGamePedestrian;
import org.vadere.state.attributes.models.AttributesTimeCost;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.PedestrianAttitudeType;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The TimeCostFunctionFactory creates the TimeCostFunctions with the currently
 * availible configurations. The Decorator-Pattern is used for the
 * TimeCostFunctions. So you can combine different TimeCostFunctions!
 * 
 * UNIT: static potential field, with F=1
 * 
 * NAVIGATION: time cost funtions which takes the density, measured by the
 * gaussian function, in count. The higher the density the higher is the
 * repulsion effect. The obstacle and the pedestrian density influences this
 * chraracteristics.
 * 
 * QUEUING: time cost funtions which takes the density, measured by the gaussian
 * function, in count. The higher the pedestrian density the higher is the
 * gravity. The obstacle density still has an repulsion effect.
 * 
 * 
 */
public class TimeCostFunctionFactory {
	/**
	 * Construct the TimeCostFunction-Decoration (combination of different time
	 * cost function), based on the attributes.
	 * 
	 * @param timeCostAttributes
	 *        the attribute that is significant for the time cost function
	 *        combinations and for their configurations
	 * @param topography
	 *        the floor of the potential field generator that uses this time
	 *        cost function
	 * @param targetId
	 *        the target id of the potential field generator target body
	 * @param scale
	 *        the scale (this should be equals to 1/gridresoulution)
	 * @return the TimeCostFunction-Decoration based on the attributes.
	 */
	public static ITimeCostFunction create(
			final AttributesTimeCost timeCostAttributes,
			final AttributesAgent attributesPedestrian,
			final Topography topography, final int targetId, final double scale) {

		switch (timeCostAttributes.getType()) {
			case UNIT:
				return new UnitTimeCostFunction();
			case NAVIGATION: {
				ITimeCostFunction timeCostObstacle = create(timeCostAttributes, topography, scale);

				IPedestrianLoadingStrategy loadingStrategy = IPedestrianLoadingStrategy.create(
						topography,
						timeCostAttributes,
						attributesPedestrian,
						targetId);

				IGaussianFilter filter = IGaussianFilter.create(
						topography.getBounds(),
						topography.getElements(Pedestrian.class),
						scale,
						timeCostAttributes.getStandardDeviation(),
						attributesPedestrian,
						loadingStrategy);

				return new TimeCostPedestrianDensity(timeCostObstacle, filter);
			}
			case NAVIGATION_GAME: {
				ITimeCostFunction timeCostObstacle = create(timeCostAttributes, topography, scale);

				IPedestrianLoadingStrategy loadingStrategy = IPedestrianLoadingStrategy.create(
						topography,
						timeCostAttributes,
						attributesPedestrian,
						targetId);

				loadingStrategy = IPedestrianLoadingStrategy.create(
						loadingStrategy,
						p -> p.getModelPedestrian(QueueingGamePedestrian.class)
								.getAttituteType() == PedestrianAttitudeType.COMPETITIVE);

				IGaussianFilter filter = IGaussianFilter.create(
						topography.getBounds(),
						topography.getElements(Pedestrian.class),
						scale,
						timeCostAttributes.getStandardDeviation(),
						attributesPedestrian,
						loadingStrategy);

				return new TimeCostPedestrianDensity(timeCostObstacle, filter);
			}
			case QUEUEING: {
				ITimeCostFunction timeCostObstacle = create(timeCostAttributes, topography, scale);
				IPedestrianLoadingStrategy loadingStrategy = IPedestrianLoadingStrategy.create();
				IGaussianFilter filter = IGaussianFilter.create(
						topography.getBounds(),
						topography.getElements(Pedestrian.class),
						scale,
						timeCostAttributes.getStandardDeviation(),
						attributesPedestrian,
						loadingStrategy);

				return new TimeCostPedestrianDensityQueuing(timeCostObstacle, timeCostAttributes, filter);
			}
			case QUEUEING_GAME: {
				ITimeCostFunction timeCostObstacle = create(timeCostAttributes, topography, scale);
				IPedestrianLoadingStrategy loadingStrategy = IPedestrianLoadingStrategy.create(
						IPedestrianLoadingStrategy.create(),
						p -> p.getModelPedestrian(QueueingGamePedestrian.class)
								.getAttituteType() == PedestrianAttitudeType.GENTLE);
				IGaussianFilter filter = IGaussianFilter.create(
						topography.getBounds(),
						topography.getElements(Pedestrian.class),
						scale,
						timeCostAttributes.getStandardDeviation(),
						attributesPedestrian,
						loadingStrategy);

				return new TimeCostPedestrianDensityQueuing(timeCostObstacle, timeCostAttributes, filter);
			}
			case OBSTACLES: {
				return create(timeCostAttributes, topography, scale);
			}
			case DISTANCE_TO_OBSTACLES:
				return new TimeCostFunctionObstacleDistance(
						new UnitTimeCostFunction(),
						p -> topography.distanceToObstacle(p),
						timeCostAttributes.getHeight(),
						timeCostAttributes.getWidth());
			default: {
				throw new IllegalArgumentException(timeCostAttributes.getType()
						+ " - no such time-cost function exists!");
			}
		}
	}

	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> ITimeCostFunction create(
			final AttributesTimeCost timeCostAttributes,
			final AttributesAgent attributesPedestrian,
			final Topography topography, final int targetId,
			IIncrementalTriangulation<V, E, F> triangulation) {

		IPedestrianLoadingStrategy loadingStrategy = IPedestrianLoadingStrategy.create(
				topography,
				timeCostAttributes,
				attributesPedestrian,
				targetId);

		ITimeCostFunction unit = new UnitTimeCostFunction();
		final double R = 0.7;
		int influenceRadius = 5;
		double dia = attributesPedestrian.getRadius() * 2.0;
		double Sp = (dia * dia * Math.sqrt(3)) * 0.5;
		double a = -1 / (2 * R * R);
		double c = 2 * Math.PI * R * R;

		ITimeCostFunctionMesh<V> pedDensityTimeCost = new ITimeCostFunctionMesh<V>() {

			private IVertexContainerDouble<V, E, F> densities = triangulation.getMesh().getDoubleVertexContainer(nameAgentDensity);
			public static final String nameAgentDensity = "agent_density";

			private double density(final double x1, final double y1, final double x2, final double y2, @Nullable final Pedestrian ped) {
				double dist = GeometryUtils.lengthSq(x1 - x2, y1 - y2);
				double density = loadingStrategy.calculateLoading(ped) * (Sp / (c)) * Math.exp(a * dist);
				return density;
			}

			@Override
			public double costAt(@NotNull final V v, @Nullable final Object caller) {
				return unit.costAt(triangulation.getMesh().toPoint(v)) + densities.getValue(v);
			}

			@Override
			public double costAt(V v) {
				return costAt(v, null);
			}

			@Override
			public double costAt(@NotNull final IPoint p) {
				F face = triangulation.locate(p).get();
				double cost = 0;
				if (!triangulation.getMesh().isBoundary(face)) {
					cost = GeometryUtilsMesh.barycentricInterpolation(face, triangulation.getMesh(),
							v -> densities.getValue(v), p.getX(), p.getY());
				}
				return unit.costAt(p) + cost;
			}

			@Override
			public boolean needsUpdate() {
				return true;
			}

			@Override
			public void update() {
				long ms = System.currentTimeMillis();
				var mesh = triangulation.getMesh();
				densities.reset();

				for (Pedestrian element : topography.getPedestrianDynamicElements().getElements()) {
					Optional<F> optional = triangulation.locateFace(element.getPosition(), element);
					assert optional.isPresent();

					if (optional.isPresent()) {
						F pedFace = optional.get();
						Predicate<V> predicate = v -> GeometryUtils.lengthSq(mesh.getX(v) - element.getPosition().x,
								mesh.getY(v) - element.getPosition().y) < influenceRadius * influenceRadius;
						Set<V> closeVertices = triangulation.getVertices(element.getPosition().getX(), element.getPosition().getY(), pedFace, predicate);
						for (V v : closeVertices) {
							double density = densities.getValue(v) + density(element.getPosition().x, element.getPosition().y, mesh.getX(v), mesh.getY(v), element);
							densities.setValue(v, density);
						}
					}
				}

				long runTime = System.currentTimeMillis() - ms;
				System.out.println("runTime of density computation = " + runTime);
			}
		};

		ITimeCostFunctionMesh<V> obstacleDensityTimeCost = new ITimeCostFunctionMesh<V>() {
			private IVertexContainerDouble<V, E, F> densities = triangulation.getMesh().getDoubleVertexContainer(nameObstacleDensity);
			public static final String nameObstacleDensity = "obstacle_density";

			private boolean updated = false;

			private double density(final double dist) {
				double density = 1/c * Math.PI * Erf.erfc(dist * Math.sqrt(-a)) / (-2 * a);
				return density;
			}

			@Override
			public double costAt(@NotNull final V v, @Nullable final Object caller) {
				return pedDensityTimeCost.costAt(v, caller) + timeCostAttributes.getObstacleDensityWeight() * densities.getValue(v);
			}

			@Override
			public double costAt(V v) {
				return costAt(v, null);
			}

			@Override
			public double costAt(@NotNull final IPoint p) {
				F face = triangulation.locate(p).get();
				double cost = 0;
				if (!triangulation.getMesh().isBoundary(face)) {
					cost = GeometryUtilsMesh.barycentricInterpolation(face, triangulation.getMesh(),
							v -> densities.getValue(v), p.getX(), p.getY());
				}
				return unit.costAt(p) + cost;
			}
			@Override
			public void update() {
				pedDensityTimeCost.update();
				if(!updated) {
					triangulation.getMesh().streamVerticesParallel()
							.filter(v -> topography.distanceToObstacle(triangulation.getMesh().toPoint(v)) < influenceRadius)
							.forEach(v -> {
								double dist = topography.distanceToObstacle(triangulation.getMesh().toPoint(v));
								double density = density(dist);
								densities.setValue(v, density);
							});
				}
				updated = true;
			}

			@Override
			public boolean needsUpdate() {
				return !updated || pedDensityTimeCost.needsUpdate();
			}
		};

		return obstacleDensityTimeCost;
	}


	private static TimeCostObstacleDensity create(
			final AttributesTimeCost timeCostAttributes,
			final Topography topography, final double scale) {
		IGaussianFilter obstacleFilter = IGaussianFilter.create(
				topography,
				scale,
				timeCostAttributes.getStandardDeviation());

		TimeCostObstacleDensity timeCostObstacle = new TimeCostObstacleDensity(
				new UnitTimeCostFunction(),
				timeCostAttributes.getObstacleDensityWeight(),
				obstacleFilter);

		return timeCostObstacle;
	}
}
