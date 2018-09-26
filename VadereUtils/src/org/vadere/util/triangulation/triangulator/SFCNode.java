package org.vadere.util.triangulation.triangulator;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * A {@link SFCNode} is part of the {@link SpaceFillingCurve} containing a
 * half-edge which referst to a specific face and a direction which indicate its traversal
 * direction with respect to the direction of the half-edge, i.e. true => in direction of the
 * half-edge, false => in the reverse direction of the half-edge.
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> generic type of the point
 * @param <V> generic type of the vertex
 * @param <E> generic type of the half-edge
 * @param <F> generic type of the face
 */
public class SFCNode<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {
	private final E edge;
	private final SFCDirection direction;

	SFCNode<P, V, E, F> next = null;
	SFCNode<P, V, E, F> prev = null;

	public SFCNode(@NotNull final E edge, @NotNull final SFCDirection direction) {
		this.edge = edge;
		this.direction = direction;
	}

	public E getEdge() {
		return edge;
	}

	public SFCDirection getDirection() {
		return direction;
	}

	void destroy() {
		next = null;
		prev = null;
	}

	@Override
	public String toString() {
		return edge.toString();
	}
}
