package org.vadere.meshing.mesh.gen;

import org.vadere.meshing.mesh.inter.IMeshSupplier;

public class PMeshSuppliert implements IMeshSupplier<PVertex, PHalfEdge, PFace> {

	public static PMeshSuppliert defaultMeshSupplier = new PMeshSuppliert();

	public PMeshSuppliert() {}

	@Override
	public PMesh get() {
		return new PMesh();
	}
}

