package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenConstrainedDelaunayTriangulator;
import org.vadere.util.geometry.shapes.IPoint;

public class PContrainedDelaunayTriangulator extends GenConstrainedDelaunayTriangulator<PVertex, PHalfEdge, PFace> {

	public PContrainedDelaunayTriangulator(
			@NotNull final PSLG pslg) {
		this(pslg, false);
	}

	public PContrainedDelaunayTriangulator(
			@NotNull final PSLG pslg,
			final boolean confirming) {
		super(() -> new PMesh(), pslg, confirming);
	}
}
