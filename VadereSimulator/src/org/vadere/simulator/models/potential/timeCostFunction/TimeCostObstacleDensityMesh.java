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
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunctionMesh;
import org.vadere.state.attributes.models.AttributesTimeCost;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.math.IDistanceFunction;

/**
 * A TimeCostFunction which reduces the travelling speed decreases with the obstacle density.
 *
 * @param <V>
 * @param <E>
 * @param <F>
 */
public class TimeCostObstacleDensityMesh<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITimeCostFunctionMesh<V> {

	public static final String nameObstacleDensity = "obstacle_density";

	private final ITimeCostFunction timeCostFunction;
	private final IDistanceFunction distanceFunction;
	private final AttributesTimeCost attributesTimeCost;
	private final IIncrementalTriangulation<V, E, F> triangulation;
	private final IVertexContainerDouble<V, E, F> densities;
	private boolean updated;

	private final double R = 2;
	private final int influenceRadius = 5;
	private final double a;
	private final double Sp;
	private final double c;

	public TimeCostObstacleDensityMesh(
			@NotNull final ITimeCostFunction timeCostFunction,
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			final AttributesTimeCost attributesTimeCost,
			final AttributesAgent attributesAgent,
			final IDistanceFunction distanceFunction){
		this.timeCostFunction = timeCostFunction;
		this.attributesTimeCost = attributesTimeCost;
		this.triangulation = triangulation;
		this.distanceFunction = distanceFunction;
		this.densities = triangulation.getMesh().getDoubleVertexContainer(nameObstacleDensity);
		this.updated = false;

		//TODO duplicated code
		double dia = attributesAgent.getRadius() * 2.0;
		Sp = (dia * dia * Math.sqrt(3)) * 0.5;
		a = -1 / (2 * R * R);
		c = 2 * Math.PI * R * R;
	}

	private double density(final double dist) {
		double density = 1/c * Math.PI * Erf.erfc(dist * Math.sqrt(-a)) / (-2 * a);
		return density;
	}

	@Override
	public double costAt(@NotNull final V v, @Nullable final Object caller) {
		return timeCostFunction.costAt(v, caller) + attributesTimeCost.getObstacleDensityWeight() * densities.getValue(v);
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
	public void update() {
		timeCostFunction.update();
		if(!updated) {
			triangulation.getMesh().streamVerticesParallel()
					.filter(v -> -distanceFunction.apply(triangulation.getMesh().toPoint(v)) < influenceRadius)
					.forEach(v -> {
						double dist = Math.max(0, -distanceFunction.apply(triangulation.getMesh().toPoint(v)));
						double density = density(dist);
						densities.setValue(v, density);
					});
		}
		updated = true;
	}

	@Override
	public boolean needsUpdate() {
		return !updated || timeCostFunction.needsUpdate();
	}
}
