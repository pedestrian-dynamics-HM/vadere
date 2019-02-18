package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.util.geometry.shapes.IPoint;

public class APMesh<P extends IPoint> extends PMesh<P, Object, Object> {

	public APMesh(@NotNull final IPointConstructor<P> pointConstructor) {
		super(pointConstructor);
	}
}
