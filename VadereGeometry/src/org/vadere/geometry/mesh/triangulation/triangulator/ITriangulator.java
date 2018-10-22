package org.vadere.geometry.mesh.triangulation.triangulator;

import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.ITriangulation;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;

/**
 * <p>A triangulator i.e. a triangle generator creates a triangulation using a certain strategy.
 * The strategy determines which point will be inserted at which time and at which position.
 * The algorithm which inserts the point is part of the {@link ITriangulator}</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> generic type of the point
 * @param <V> generic type of the vertex
 * @param <E> generic type of the half-edge
 * @param <F> generic type of the face
 */
@FunctionalInterface
public interface ITriangulator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {

	/**
	 * <p>Returns the generated triangulation.</p>
	 *
	 * @return the generated triangulation
	 */
	ITriangulation<P, V, E, F> generate();
}
