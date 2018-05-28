package org.vadere.util.triangulation.triangulator;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * Created by bzoennchen on 25.05.18.
 */
public class SFCNode<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {
	private final E edge;
	private final SFCDirection direction;
	private boolean refine;

	SFCNode<P, V, E, F> next = null;
	SFCNode<P, V, E, F> prev = null;

	public SFCNode(@NotNull final E edge, @NotNull final SFCDirection direction, final boolean refine) {
		this.edge = edge;
		this.direction = direction;
		this.refine = refine;
	}

	public SFCNode(@NotNull final E edge, @NotNull final SFCDirection direction) {
		this.edge = edge;
		this.direction = direction;
		this.refine = false;
	}

	public E getEdge() {
		return edge;
	}

	public SFCDirection getDirection() {
		return direction;
	}

	public void setRefine(final boolean refine) {
		this.refine = refine;
	}

	public boolean isRefine() {
		return refine;
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
