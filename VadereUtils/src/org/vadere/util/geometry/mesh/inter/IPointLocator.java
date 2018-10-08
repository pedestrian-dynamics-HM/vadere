package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;
import java.util.Optional;

/**
 * A point-locator {@link IPointLocator} implements one of the strategies to find a specific triangle
 * represented by a face {@link F} inside a mesh {@link ITriangulation}. i.e. a set of connected non-overlapping
 * triangles including holes. The most famous strategies are so called triangle walks described in:
 *
 * + Walking in a Triangulation (devillers-2001)
 * + The Delaunay Hierarchy (devillers-2002)
 * + Fast randomized point location without preprocessing in two- and three-dimensional Delaunay triangulations (mucke-1999)
 * + The Delaunay Tree see Computational Geometry: Algorithms and Applications (berg-2008) page 191
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IPointLocator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends ITriEventListener<P, V, E, F> {

	/**
	 * Starts the point location of the point and returns the face which is found.
	 *
	 * Assumption: the point is inside the {@link ITriangulation<P, V, E, F>}.
	 *
	 * @param point     the point
	 * @return the face containing the point
	 */
	F locatePoint(final P point);

	/**
	 * Starts the point location of the point and returns the face which is found.
	 *
	 * @param point     the point
	 * @return the face containing the point
	 */
	Optional<F> locate(final P point);

	/**
	 * Starts the point location of the point and returns the face which is found.
	 *
	 * @return the face containing the point
	 */
	Optional<F> locate(final double x, final double y);

	/**
	 * Returns its type.
	 *
	 * @return its type
	 */
	Type getType();

	/**
	 * Types of implemented point location algorithms {@link IPointLocator}.
	 */
	enum Type {
		DELAUNAY_TREE,
		DELAUNAY_HIERARCHY,
		JUMP_AND_WALK,      // preferable!
		BASE
	}
}
