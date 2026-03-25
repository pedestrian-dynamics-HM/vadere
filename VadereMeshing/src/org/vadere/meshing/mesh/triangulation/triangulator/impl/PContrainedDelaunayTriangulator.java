package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.*;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles.PTriangleMeshBuilder;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenConstrainedDelaunayTriangulator;
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
		super(PTriangleMeshBuilder::new, pslg, confirming);
	}

	public PContrainedDelaunayTriangulator(
			@NotNull final IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation,
			@NotNull final Collection<VLine> constrains,
			final boolean confirming) {
		super(triangulation, constrains, confirming);
	}
}
