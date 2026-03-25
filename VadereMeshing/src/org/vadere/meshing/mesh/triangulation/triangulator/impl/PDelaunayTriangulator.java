package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.*;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles.PTriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenDelaunayTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;
import java.util.function.Supplier;

public class PDelaunayTriangulator extends GenDelaunayTriangulator<PVertex, PHalfEdge, PFace> {

	public PDelaunayTriangulator(
			@NotNull final Supplier<ITriangleMeshBuilder<PVertex, PHalfEdge, PFace>> emptyMeshSupplier,
			@NotNull final VRectangle bound,
			@NotNull final Collection<? extends IPoint> pointSet) {
		super(emptyMeshSupplier, bound, pointSet);
	}

	public PDelaunayTriangulator(
			@NotNull final Collection<? extends IPoint> pointSet) {
		super(PTriangleMeshBuilder::new, pointSet);
	}
}
