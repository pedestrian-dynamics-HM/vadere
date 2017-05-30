package org.vadere.util.geometry.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class VPFace extends PFace<VPoint> {
	VPFace(@NotNull VPHalfEdge edge) {
		super(edge);
	}
}
