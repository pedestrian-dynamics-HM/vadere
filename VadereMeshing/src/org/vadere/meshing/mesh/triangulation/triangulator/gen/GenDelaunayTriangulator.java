package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;

public class GenDelaunayTriangulator<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements ITriangulator<P, CE, CF, V, E, F> {

	private final Collection<P> pointSet;
	private IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation;
	private boolean generated;

	public GenDelaunayTriangulator(@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
	                               @NotNull final VRectangle bound,
	                               @NotNull final Collection<P> pointSet) {
		this.pointSet = pointSet;
		this.triangulation = new IncrementalTriangulation<>(mesh, bound, halfEdge -> true);
		this.generated = false;
	}

	public GenDelaunayTriangulator(@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
	                               @NotNull final Collection<P> pointSet) {
		this.pointSet = pointSet;
		this.triangulation = new IncrementalTriangulation<>(mesh, GeometryUtils.bound(pointSet, GeometryUtils.DOUBLE_EPS), halfEdge -> true);
		this.generated = false;
	}

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> generate() {
		if(!generated) {
			triangulation.init();
			triangulation.insert(pointSet);
			triangulation.finish();
			generated = true;
		}
		return triangulation;
	}

	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return triangulation.getMesh();
	}
}
