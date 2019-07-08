package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenDelaunayTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;

public class PDelaunayTriangulator extends GenDelaunayTriangulator<PVertex, PHalfEdge, PFace> {

	public PDelaunayTriangulator(
			@NotNull final PMesh mesh,
			@NotNull final VRectangle bound,
			@NotNull final Collection<? extends IPoint> pointSet) {
		super(mesh, bound, pointSet);
	}

	public PDelaunayTriangulator(
			@NotNull final PMesh mesh,
			@NotNull final Collection<? extends IPoint> pointSet) {
		super(mesh, pointSet);
	}

	public PDelaunayTriangulator(
			@NotNull final Collection<? extends IPoint> pointSet) {
		super(new PMesh(), pointSet);
	}
}
