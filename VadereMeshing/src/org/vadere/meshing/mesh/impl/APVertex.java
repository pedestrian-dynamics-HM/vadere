package org.vadere.meshing.mesh.impl;

import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class APVertex<P extends IPoint> extends PVertex<P, Object, Object> {

	public APVertex(P point) {
		super(point);
	}
}
