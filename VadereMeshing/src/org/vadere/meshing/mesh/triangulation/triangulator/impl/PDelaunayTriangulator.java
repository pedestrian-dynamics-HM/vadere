package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenDelaunayTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;
import java.util.Set;

public class PDelaunayTriangulator<P extends IPoint, CE, CF> extends GenDelaunayTriangulator<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {

	public PDelaunayTriangulator(
			@NotNull final PMesh<P, CE, CF> mesh,
			@NotNull final VRectangle bound,
			@NotNull final Collection<P> pointSet) {
		super(mesh, bound, pointSet);
	}

	public PDelaunayTriangulator(
			@NotNull final PMesh<P, CE, CF> mesh,
			@NotNull final Collection<P> pointSet) {
		super(mesh, pointSet);
	}

	public PDelaunayTriangulator(
			@NotNull final Collection<P> pointSet,
			@NotNull final IPointConstructor<P> pointConstructor) {
		super(new PMesh<>(pointConstructor), pointSet);
	}
}
