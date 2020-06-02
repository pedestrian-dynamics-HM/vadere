package org.vadere.simulator.models.potential.timeCostFunction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.utils.math.GeometryUtilsMesh;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunctionMesh;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.math.MathUtil;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A TimeCostFunction which reduces the travelling speed decreases with the obstacle density.
 *
 * @param <V>
 * @param <E>
 * @param <F>
 */
public class TimeCostPedestrianDensityQueueingMesh<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITimeCostFunctionMesh<V> {

	public static final String nameObstacleDensity = "agent_density";

	private final ITimeCostFunction timeCostFunction;
	private final Topography topography;
	private final IIncrementalTriangulation<V, E, F> triangulation;
	private final IVertexContainerDouble<V, E, F> densities;
	private final IPedestrianLoadingStrategy loadingStrategy;
	private boolean updated;

	private final double R = 0.7;
	private final int influenceRadius = 5;
	private final double a;
	private final double Sp;
	private final double c;

	public TimeCostPedestrianDensityQueueingMesh(
			@NotNull final ITimeCostFunction timeCostFunction,
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final IPedestrianLoadingStrategy loadingStrategy,
			final AttributesAgent attributesAgent,
			final Topography topography){
		this.timeCostFunction = timeCostFunction;
		this.loadingStrategy = loadingStrategy;
		this.triangulation = triangulation;
		this.topography = topography;
		this.densities = triangulation.getMesh().getDoubleVertexContainer(nameObstacleDensity);
		this.updated = false;

		//TODO duplicated code
		double dia = attributesAgent.getRadius() * 2.0;
		Sp = (dia * dia * Math.sqrt(3)) * 0.5;
		a = -1 / (2 * R * R);
		c = 2 * Math.PI * R * R;
	}

	private double density(final double x1, final double y1, final double x2, final double y2, @Nullable final Pedestrian ped) {
		double dist = GeometryUtils.lengthSq(x1 - x2, y1 - y2);
		double density = loadingStrategy.calculateLoading(ped) * (Sp / (c)) * Math.exp(a * dist);
		return density;
	}

	@Override
	public double costAt(@NotNull final V v, @Nullable final Object caller) {
		double cost = densities.getValue(v);
		cost = Math.min(cost, 1.0 - MathUtil.EPSILON);
		return timeCostFunction.costAt(triangulation.getMesh().toPoint(v)) - cost;
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
		return timeCostFunction.costAt(p) + cost;
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
}
