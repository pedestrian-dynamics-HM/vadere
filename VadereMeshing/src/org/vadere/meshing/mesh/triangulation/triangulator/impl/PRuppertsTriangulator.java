package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class PRuppertsTriangulator<P extends IPoint, CE, CF>  extends GenRuppertsTriangulator<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {
	public PRuppertsTriangulator(
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final IPointConstructor<P> pointConstructor) {
		super(() -> new PMesh<>(pointConstructor), bound, constrains, Collections.EMPTY_SET);
	}

	public PRuppertsTriangulator(
			@NotNull final Collection<VLine> constrains,
			@NotNull final VPolygon bound,
			@NotNull final IPointConstructor<P> pointConstructor) {
		super(() -> new PMesh<>(pointConstructor), constrains, bound);
	}

	public PRuppertsTriangulator(
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final IPointConstructor<P> pointConstructor,
			final double minAngle) {
		super(() -> new PMesh<>(pointConstructor), bound, constrains, Collections.EMPTY_SET, minAngle);
	}

	public PRuppertsTriangulator(
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final PMesh<P, CE, CF> mesh,
			final double minAngle) {
		super(() -> mesh, bound, constrains, Collections.EMPTY_SET, minAngle);
	}

	public PRuppertsTriangulator(
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final PMesh<P, CE, CF> mesh,
			final double minAngle,
			final boolean createHoles) {
		super(() -> mesh, bound, constrains, Collections.EMPTY_SET, minAngle, createHoles);
	}
}
