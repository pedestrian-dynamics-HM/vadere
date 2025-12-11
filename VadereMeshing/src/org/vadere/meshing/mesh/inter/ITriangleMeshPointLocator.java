package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.util.geometry.shapes.IPoint;
import java.util.Optional;

/**
 * A point-locator {@link ITriangleMeshPointLocator} implements one of the strategies to find a specific triangle
 * represented by a face {@link F} inside a mesh {@link IIncrementalTriangulation}. i.e. a set of connected non-overlapping
 * triangles including holes. The most famous strategies are so called triangle walks described in:
 * <ul>
 *     <li>Walking in a Triangulation (devillers-2001)</li>
 *     <li>The Delaunay Hierarchy (devillers-2002)</li>
 *     <li>Fast randomized point location without preprocessing in two- and three-dimensional Delaunay triangulations (mucke-1999)</li>
 *     <li>The Delaunay Tree see Computational Geometry: Algorithms and Applications (berg-2008) page 191</li>
 * </ul>
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface ITriangleMeshPointLocator<V extends IVertex, E extends IHalfEdge, F extends IFace> extends ITriEventListener<V, E, F> {

	/**
	 * Starts the point location of the point and returns the face which is found.
	 *
	 * Assumption: the point is inside the {@link IIncrementalTriangulation}.
	 *
	 * @param point     the point
	 * @return the face containing the point
	 */
	F locatePoint(@NotNull final IPoint point);

	default F locatePoint(@NotNull final IPoint point, final Object caller) {
		return locatePoint(point);
	}

	/**
	 * Starts the point location of the point and returns the face which is found.
	 *
	 * @param point     the point
	 * @return the face containing the point
	 */
	Optional<F> locate(@NotNull final IPoint point);

	default Optional<F> locate(@NotNull final IPoint point, final Object caller) {
		return locate(point);
	}

	/**
	 * Starts the point (x,y) location of the point and returns the face which is found.
	 *
	 * @param x x-coordinate of the point
	 * @param y y-coordinate of the point
	 * @return the face containing the point
	 */
	Optional<F> locate(final double x, final double y);

	default Optional<F> locate(final double x, final double y, final Object caller) {
		return locate(x, y);
	}

	/**
	 * Returns its type.
	 *
	 * @return its type
	 */
	Type getType();

	ITriangleMeshPointLocator<V, E, F> withCache();
	ITriangleMeshPointLocator<V, E, F> getUncachedLocator();
	boolean isCached();

	/**
	 * Types of implemented point location algorithms {@link ITriangleMeshPointLocator}.
	 */
	enum Type {
		JUMP_AND_WALK,      // preferable!
	}



	ITriangleMeshPointLocator<V, E, F> copyFor(ITriangleMesh<V, E, F> mesh);
}
