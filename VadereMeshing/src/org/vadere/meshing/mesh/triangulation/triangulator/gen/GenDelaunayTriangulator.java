package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IEmptyMeshSupplier;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;

public class GenDelaunayTriangulator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangulator<V, E, F> {

	private final Collection<? extends IPoint> pointSet;
	private IIncrementalTriangulation<V, E, F> triangulation;
	private boolean generated;

	public GenDelaunayTriangulator(@NotNull final IEmptyMeshSupplier<V, E, F> emptyMeshSupplier,
								   @NotNull final VRectangle bound,
								   @NotNull final Collection<? extends IPoint> pointSet) {
		this.pointSet = pointSet;
		this.triangulation = IncrementalTriangulation.fromEmptyMesh(emptyMeshSupplier.get(), bound);
		this.generated = false;
	}

	public GenDelaunayTriangulator(@NotNull final IEmptyMeshSupplier<V, E, F> emptyMeshSupplier,
	                               @NotNull final Collection<? extends IPoint> pointSet) {
		this.pointSet = pointSet;
		this.triangulation = IncrementalTriangulation.fromEmptyMesh(emptyMeshSupplier.get(), GeometryUtils.boundRelative(pointSet));
		this.generated = false;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate() {
		return generate(true);
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		if(!generated) {
			triangulation.init();
			triangulation.insert(pointSet);

			if(finalize) {
				triangulation.finish();
			}
			generated = true;
		}
		return triangulation;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return triangulation.getMeshDataStorage();
	}

	@Override
	public IMeshWithDataStorage<V, E, F> getMeshWithDataStorage() {
		return triangulation.getMeshWithDataStorage();
	}
}
