package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.impl.PTriangulation;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenConstrainedDelaunayTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;

import java.util.Collection;

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

	public PContrainedDelaunayTriangulator(
			@NotNull final IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation,
			@NotNull final Collection<VLine> constrains,
			final boolean confirming) {
		super(triangulation, constrains, confirming);
	}
}
