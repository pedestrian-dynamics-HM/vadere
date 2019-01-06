package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class APHalfEdge<P extends IPoint> extends PHalfEdge<P, Object, Object> {

	public APHalfEdge(@NotNull final APVertex<P> end, final @NotNull APFace<P> face) {
		super(end, face);
	}

	protected APHalfEdge(@NotNull final APVertex<P> end) {
		super(end);
	}
}
