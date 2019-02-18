package org.vadere.meshing.mesh.impl;

import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.shapes.IPoint;

public class APMeshSupplier<P extends IPoint> implements IMeshSupplier<P, Object, Object, APVertex<P>, APHalfEdge<P>, APFace<P>> {

	@Override
	public IMesh<P, Object, Object, APVertex<P>, APHalfEdge<P>, APFace<P>> get() {
		return null;
	}
}
