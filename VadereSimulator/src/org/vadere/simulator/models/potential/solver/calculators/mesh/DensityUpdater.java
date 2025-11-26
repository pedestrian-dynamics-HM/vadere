package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.ITriangleMeshPointLocator;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshVertices;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyTriConnectivity;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.scenario.IMoveDynamicElementListener;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This callback class updates the so called agent/pedestrian density every time an agent moves,
 * i.e. whenever this listener is called. Therefore, the listener has to be added in the first place.
 * {@link DensityUpdater#nameAgentDensity} is the name of the container of the mesh where the agent
 * density (a {@link Double}) value is saved.
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 *
 * @author Benedikt Zoennchen
 */
public class DensityUpdater<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IMoveDynamicElementListener {

	public static final String nameAgentDensity = "agent_density";

	private final ITriangleMeshWithDataStorage<V, E, F> triangulation;
	private ITriangleMeshVertices<V, E, F> vertices;
	private IReadOnlyTriConnectivity<V, E, F> connectivity;

	private double influenceRadius = 9;
	private IVertexContainerDouble<V, E, F> densities;
    private ITriangleMeshPointLocator<V, E, F> pointLocator;
    private IPedestrianLoadingStrategy loadingStrategy;

	private final static double R = 0.7;

	public DensityUpdater(@NotNull final ITriangleMeshWithDataStorage<V, E, F> triangulation, @NotNull ITriangleMeshPointLocator<V, E, F> pointLocator, @Nullable final IPedestrianLoadingStrategy loadingStrategy) {
		this.triangulation = triangulation;
		this.densities = triangulation.getDataStorage().getDoubleVertexContainer(nameAgentDensity, triangulation.getMesh());
        this.pointLocator = pointLocator;
        this.loadingStrategy = loadingStrategy;
		this.vertices = triangulation.getMesh().vertices();
		connectivity = triangulation.getMesh().readConnectivity();
	}

	public DensityUpdater(@NotNull final ITriangleMeshWithDataStorage<V, E, F> triangulation, @NotNull ITriangleMeshPointLocator<V, E, F> pointLocator) {
		this(triangulation, pointLocator, IPedestrianLoadingStrategy.create(1.0));
	}

	@Override
	public void moveElement(@NotNull final Pedestrian element, @NotNull final VPoint oldPosition) {
		Optional<F> optional = pointLocator.locate(oldPosition, element);
		if (optional.isPresent()) {
			F pedFace = optional.get();

			Predicate<V> predicate = v -> GeometryUtils.lengthSq(vertices.getX(v) - oldPosition.getX(),
					vertices.getY(v) - oldPosition.getY()) < influenceRadius * influenceRadius;

			Set<V> closeVertices = connectivity.getVertices(oldPosition.x, oldPosition.y, pedFace, predicate);
			for (V v : closeVertices) {
				double density = densities.getValue(v) - density(oldPosition.x, oldPosition.y, vertices.getX(v), vertices.getY(v), element);
				densities.setValue(v, Math.max(0, density));
			}
		}

		optional = pointLocator.locate(element.getPosition(), element);
		if (optional.isPresent()) {
			F pedFace = optional.get();

			Predicate<V> predicate = v -> GeometryUtils.lengthSq(vertices.getX(v) - element.getPosition().x,
					vertices.getY(v) - element.getPosition().y) < influenceRadius * influenceRadius;

			Set<V> closeVertices = connectivity.getVertices(element.getPosition().getX(), element.getPosition().getY(), pedFace, predicate);
			for (V v : closeVertices) {
				double density = densities.getValue(v) + density(element.getPosition().x, element.getPosition().y, vertices.getX(v), vertices.getY(v), element);
				densities.setValue(v, density);
			}
		}
	}

	private double density(final double x1, final double y1, final double x2, final double y2, @Nullable final Pedestrian ped) {
		double dist = GeometryUtils.lengthSq(x1 - x2,y1 - y2);
		double density = loadingStrategy.calculateLoading(ped) * 1/(2 * Math.PI * R * R) * Math.exp(- 1 / (2 * R * R) * dist);
		return density;
	}

	private IMesh<V, E, F> getMesh() {
		return triangulation.getMesh();
	}
}
