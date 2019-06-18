package org.vadere.meshing.mesh.triangulation.triangulator.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

/**
 * A {@link IPlacementStrategy} computes insertion points based on some implemented strategy
 * such as the Delaunay-refinement strategy i.e. the insertion point is the circumcenter of some
 * Delaunay triangle.
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IPlacementStrategy<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	IMesh<V, E, F> getMesh();

	default VPoint computePlacement(@NotNull final E edge) {
		return computePlacement(edge, getMesh().toTriangle(getMesh().getFace(edge)));
	}


	VPoint computePlacement(@NotNull final E edge, final VTriangle triangle);
}
