package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.*;
import org.vadere.meshing.mesh.inter.IEmptyMeshSupplier;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenDelaunayTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;

public class PDelaunayTriangulator extends GenDelaunayTriangulator<PVertex, PHalfEdge, PFace> {

	public PDelaunayTriangulator(
			@NotNull final IEmptyMeshSupplier<PVertex, PHalfEdge, PFace> emptyMeshSupplier,
			@NotNull final VRectangle bound,
			@NotNull final Collection<? extends IPoint> pointSet) {
		super(emptyMeshSupplier, bound, pointSet);
	}

	public PDelaunayTriangulator(
			@NotNull final Collection<? extends IPoint> pointSet) {
		super(PMeshWithDataStorage::constructEmpty, pointSet);
	}
}
