package org.vadere.simulator.models.potential.timeCostFunction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRegularRefinement;
import org.vadere.meshing.utils.io.IOUtils;
import org.vadere.meshing.utils.math.GeometryUtilsMesh;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunctionMesh;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VTriangle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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
public class TimeCostPedestrianDensityMesh<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITimeCostFunctionMesh<V> {

	public static final String nameAgentDensity = "agent_density";

	private final ITimeCostFunctionMesh<V> timeCostFunction;
	private final Topography topography;
	private final IIncrementalTriangulation<V, E, F> triangulation;
	private final IVertexContainerDouble<V, E, F> densities;
	private final IPedestrianLoadingStrategy loadingStrategy;
	private boolean updated;
	private GenRegularRefinement<V, E, F> refiner;

	private final double influenceRadius = 7.0;

	// to debug
	//private MeshPanel<V, E, F> debugPanel;
	private int step;
	private BufferedWriter meshWriter;

	public TimeCostPedestrianDensityMesh(
			@NotNull final ITimeCostFunctionMesh<V> timeCostFunction,
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final IPedestrianLoadingStrategy loadingStrategy,
			final AttributesAgent attributesAgent,
			final Topography topography){
		this.timeCostFunction = timeCostFunction;
		this.loadingStrategy = loadingStrategy;
		this.triangulation = triangulation;
		this.topography = topography;
		this.densities = triangulation.getMesh().getDoubleVertexContainer(nameAgentDensity);
		this.updated = false;
		this.refiner = new GenRegularRefinement<>(triangulation, e -> false);
		this.refiner.setCoarsePredicate(v -> coarse(v));
		this.refiner.setEdgeRefinementPredicate(e -> refine(e));
		this.step = 0;
		//TODO duplicated code

		try {
			this.meshWriter = IOUtils.getWriter("floorField_densities.txt", new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		//debugPanel = new MeshPanel<>(triangulation.getMesh(), 1000, 1000);
		//debugPanel.display("debug");
	}

	private void refineMesh() {
		long ms = System.currentTimeMillis();
		refiner.coarse();
		//debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());
		refiner.refine();
		long runTime = System.currentTimeMillis() - ms;
		refiner.getMesh().garbageCollection();
		//debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());
		//System.out.println("runTime refinement = " + runTime);
	}

	private boolean coarse(@NotNull final V vertex) {
		//return true;
		for(Pedestrian pedestrian : topography.getPedestrianDynamicElements().getElements()) {
			if(pedestrian.getPosition().distanceSq(triangulation.getMesh().toPoint(vertex)) > influenceRadius * influenceRadius) {
				return true;
			}
		}
		return false;
	}

	private boolean refine(@NotNull final E e) {
		//return refiner.getLevel(e) < 2;
		if(!triangulation.getMesh().isBoundary(e)) {
			VTriangle triangle = triangulation.getMesh().toTriangle(triangulation.getMesh().getFace(e));
			if(/*!refiner.isGreen(e) || */triangulation.getMesh().toLine(e).length() > 1.0) {
				for(Pedestrian pedestrian : topography.getPedestrianDynamicElements().getElements()) {
					if(pedestrian.getPosition().distanceSq(triangle.midPoint()) < influenceRadius * influenceRadius) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public double costAt(@NotNull final V v, @Nullable final Object caller) {
		return timeCostFunction.costAt(v) + densities.getValue(v);
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
		timeCostFunction.update();
		if(step % 10 == 0) {
			//refineMesh();
		}
		step++;

		//long ms = System.currentTimeMillis();


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
					double density = densities.getValue(v) + loadingStrategy.calculateLoading(element) / (influenceRadius * influenceRadius * Math.PI);
					densities.setValue(v, density);
				}
			}
		}

		try {
			this.meshWriter.write(triangulation.getMesh().toPythonValues(v -> densities.getValue(v) + triangulation.getMesh().getDoubleData(v, TimeCostObstacleDensityMesh.nameObstacleDensity)));
			this.meshWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//long runTime = System.currentTimeMillis() - ms;
		//System.out.println("runTime of density computation = " + runTime);
	}
}
