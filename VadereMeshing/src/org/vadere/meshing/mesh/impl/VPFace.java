package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class VPFace extends APFace<VPoint> {
	VPFace(@NotNull VPHalfEdge edge) {
		super(edge);
	}
}
