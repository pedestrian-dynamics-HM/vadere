package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

public class PMeshSuppliert<P extends IPoint, CE, CF> implements IMeshSupplier<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {

	public static PMeshSuppliert<VPoint, Object, Object> defaultMeshSupplier = new PMeshSuppliert<>(IPointConstructor.pointConstructorVPoint);

	private final IPointConstructor<P> pointConstructor;

	public PMeshSuppliert(@NotNull final IPointConstructor<P> pointConstructor) {
		this.pointConstructor = pointConstructor;
	}

	@Override
	public PMesh<P, CE, CF> get() {
		return new PMesh<>(pointConstructor);
	}
}

