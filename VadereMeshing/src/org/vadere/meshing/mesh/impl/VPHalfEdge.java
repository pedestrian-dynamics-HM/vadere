package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class VPHalfEdge extends APHalfEdge<VPoint> {

	public VPHalfEdge(@NotNull final VPVertex end, final @NotNull VPFace face) {
		super(end, face);
	}

	protected VPHalfEdge(@NotNull final VPVertex end) {
		super(end);
	}

}
