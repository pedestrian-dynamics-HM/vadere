package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.util.geometry.shapes.IPoint;

public class APFace<P extends IPoint> extends PFace<P, Object, Object> {

	APFace(@NotNull APHalfEdge<P> edge) {
		super(edge);
	}
}
