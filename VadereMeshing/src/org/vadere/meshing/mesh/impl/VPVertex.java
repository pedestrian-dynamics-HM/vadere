package org.vadere.meshing.mesh.impl;

import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class VPVertex extends PVertex<VPoint> {

	public VPVertex(VPoint point) {
		super(point);
	}
}
