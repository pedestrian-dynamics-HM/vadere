package org.vadere.util.geometry.mesh.impl;

import org.vadere.util.geometry.mesh.gen.PMesh;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class VPMesh extends PMesh<VPoint> {
	public VPMesh() {
		super((x,y) -> new VPoint(x, y));
	}
}