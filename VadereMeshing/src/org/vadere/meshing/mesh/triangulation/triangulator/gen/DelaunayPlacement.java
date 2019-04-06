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
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class DelaunayPlacement<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements IPlacementStrategy<P, CE, CF, V, E ,F> {
	private IMesh<P, CE, CF, V, E, F> mesh;

	public DelaunayPlacement(@NotNull final IMesh<P, CE, CF, V, E, F> mesh) {
		this.mesh = mesh;
	}

	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return mesh;
	}

	@Override
	public VPoint computePlacement(@NotNull final E edge, @NotNull final VTriangle triangle) {
		return triangle.getCircumcenter();
	}
}
