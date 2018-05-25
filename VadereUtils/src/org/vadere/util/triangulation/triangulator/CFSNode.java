package org.vadere.util.triangulation.triangulator;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * Created by bzoennchen on 25.05.18.
 */
public class CFSNode<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {
	private final E edge;
	private final CFSDirection direction;
	private boolean refine;

	public CFSNode(@NotNull final E edge, @NotNull final CFSDirection direction, final boolean refine) {
		this.edge = edge;
		this.direction = direction;
		this.refine = refine;
	}

	public CFSNode(@NotNull final E edge, @NotNull final CFSDirection direction) {
		this.edge = edge;
		this.direction = direction;
		this.refine = false;
	}

	public E getEdge() {
		return edge;
	}

	public CFSDirection getDirection() {
		return direction;
	}

	public void setRefine(final boolean refine) {
		this.refine = refine;
	}

	public boolean isRefine() {
		return refine;
	}
}
