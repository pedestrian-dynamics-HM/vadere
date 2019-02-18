package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * <p>A {@link SFCNode} is part of the {@link GenSpaceFillingCurve} containing a
 * half-edge which refers to a specific face and a direction which indicate its traversal
 * direction with respect to the direction of the half-edge, i.e. true means in direction of the
 * half-edge, false means in the reverse direction of the half-edge.</p>
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
public class SFCNode<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> {
	private final E edge;
	private final SFCDirection direction;

	SFCNode<P, CE, CF, V, E, F> next = null;
	SFCNode<P, CE, CF, V, E, F> prev = null;

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
