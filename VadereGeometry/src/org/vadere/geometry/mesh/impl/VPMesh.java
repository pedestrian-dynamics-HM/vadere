package org.vadere.geometry.mesh.impl;

import org.vadere.geometry.mesh.gen.PMesh;
import org.vadere.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class VPMesh extends PMesh<VPoint> {
	public VPMesh() {
		super((x,y) -> new VPoint(x, y));
	}
}