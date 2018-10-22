package org.vadere.geometry.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.mesh.gen.PFace;
import org.vadere.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class VPFace extends PFace<VPoint> {
	VPFace(@NotNull VPHalfEdge edge) {
		super(edge);
	}
}
