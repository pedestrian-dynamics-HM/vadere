package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IPlacementStrategy;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

/**
 * Computes insertion point based on the Delaunay criterion, that is
 * the insertion point is the circumcenter of a triangle.
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class DelaunayPlacement<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPlacementStrategy<V, E ,F> {
	private IMesh<V, E, F> mesh;

	public DelaunayPlacement(@NotNull final IMesh<V, E, F> mesh) {
		this.mesh = mesh;
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return mesh;
	}

	@Override
	public VPoint computePlacement(@NotNull final E edge, @NotNull final VTriangle triangle) {
		return triangle.getCircumcenter();
	}
}
