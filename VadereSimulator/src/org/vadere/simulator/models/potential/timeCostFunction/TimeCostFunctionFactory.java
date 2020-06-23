package org.vadere.simulator.models.potential.timeCostFunction;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.simulator.models.density.IGaussianFilter;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.simulator.models.queuing.QueueingGamePedestrian;
import org.vadere.state.attributes.models.AttributesTimeCost;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.PedestrianAttitudeType;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;

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

		switch (timeCostAttributes.getType()) {
			case NAVIGATION: {
				IPedestrianLoadingStrategy loadingStrategy = IPedestrianLoadingStrategy.create(
						topography,
						timeCostAttributes,
						attributesPedestrian,
						targetId);

				ITimeCostFunction unit = new UnitTimeCostFunction();

				TimeCostObstacleDensityMesh<V, E, F> timeCostObstacleDensity = new TimeCostObstacleDensityMesh<>(
						unit,
						triangulation,
						timeCostAttributes,
						attributesPedestrian,
						p -> -topography.distanceToObstacle(p)
				);

				TimeCostGaussianPedestrianDensityMesh<V, E, F> timeCostPedestrianDensityMesh = new TimeCostGaussianPedestrianDensityMesh<>(
						timeCostObstacleDensity,
						triangulation,
						loadingStrategy,
						attributesPedestrian,
						topography
				);
				return timeCostPedestrianDensityMesh;
			}
			case QUEUEING: {
				ITimeCostFunction unit = new UnitTimeCostFunction();

				TimeCostObstacleDensityMesh<V, E, F> timeCostObstacleDensity = new TimeCostObstacleDensityMesh<>(
						unit,
						triangulation,
						timeCostAttributes,
						attributesPedestrian,
						p -> -topography.distanceToObstacle(p)
				);

				TimeCostPedestrianDensityQueueingMesh<V, E, F> timeCostPedestrianDensityQueueingMesh = new TimeCostPedestrianDensityQueueingMesh<>(
						timeCostObstacleDensity,
						triangulation,
						IPedestrianLoadingStrategy.create(timeCostAttributes.getQueueWidthLoading()),
						attributesPedestrian,
						topography
				);
				return timeCostPedestrianDensityQueueingMesh;
			}
			case OBSTACLES: {
				ITimeCostFunction unit = new UnitTimeCostFunction();

				TimeCostObstacleDensityMesh<V, E, F> timeCostObstacleDensity = new TimeCostObstacleDensityMesh<>(
						unit,
						triangulation,
						timeCostAttributes,
						attributesPedestrian,
						p -> -topography.distanceToObstacle(p)
				);
				return timeCostObstacleDensity;
			} case UNIT: return new UnitTimeCostFunction();
			default: {
				throw new IllegalArgumentException(timeCostAttributes.getType()
						+ " - no such time-cost function exists!");
			}
		}
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
