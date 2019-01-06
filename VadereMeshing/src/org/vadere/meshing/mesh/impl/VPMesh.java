package org.vadere.meshing.mesh.impl;

import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class VPMesh extends PMesh<VPoint, Object, Object> {
	public VPMesh() {
		super((x,y) -> new VPoint(x, y));
	}
}