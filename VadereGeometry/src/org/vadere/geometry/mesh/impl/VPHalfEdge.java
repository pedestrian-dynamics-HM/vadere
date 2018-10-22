package org.vadere.geometry.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.mesh.gen.PHalfEdge;
import org.vadere.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class VPHalfEdge extends PHalfEdge<VPoint> {

	public VPHalfEdge(@NotNull final VPVertex end, final @NotNull VPFace face) {
		super(end, face);
	}

	protected VPHalfEdge(@NotNull final VPVertex end) {
		super(end);
	}
}
