package org.vadere.meshing.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;
import java.util.Optional;

/**
 * A point-locator {@link IPointLocator} implements one of the strategies to find a specific triangle
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
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IPointLocator<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> extends ITriEventListener<P, CE, CF, V, E, F> {

	/**
	 * Starts the point location of the point and returns the face which is found.
	 *
	 * Assumption: the point is inside the {@link IIncrementalTriangulation}.
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
	 * Starts the point (x,y) location of the point and returns the face which is found.
	 *
	 * @param x x-coordinate of the point
	 * @param y y-coordinate of the point
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
