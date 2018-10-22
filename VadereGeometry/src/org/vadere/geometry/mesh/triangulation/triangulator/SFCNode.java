package org.vadere.geometry.mesh.triangulation.triangulator;

import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;

/**
 * <p>A {@link SFCNode} is part of the {@link SpaceFillingCurve} containing a
 * half-edge which refers to a specific face and a direction which indicate its traversal
 * direction with respect to the direction of the half-edge, i.e. true means in direction of the
 * half-edge, false means in the reverse direction of the half-edge.</p>
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
