package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenConstrainedDelaunayTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public class PContrainedDelaunayTriangulator<P extends IPoint, CE, CF> extends GenConstrainedDelaunayTriangulator<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {

	public PContrainedDelaunayTriangulator(
			@NotNull final VRectangle bound,
			@NotNull final Collection<VLine> constrains,
			@NotNull final Collection<P> points,
			@NotNull final IPointConstructor<P> pointConstructor) {
		super(() -> new PMesh<>(pointConstructor), bound, constrains, points);
	}

	public PContrainedDelaunayTriangulator(
			@NotNull final Collection<VLine> constrains,
			@NotNull final IPointConstructor<P> pointConstructor) {
		this(constrains, pointConstructor,false);
	}

	public PContrainedDelaunayTriangulator(
			@NotNull final Collection<VLine> constrains,
			@NotNull final IPointConstructor<P> pointConstructor,
			final boolean confirming) {
		super(() -> new PMesh<>(pointConstructor), constrains, confirming);
	}
}
