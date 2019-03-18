package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenDelaunayRefinement;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.Collection;
import java.util.function.Function;

public class PDelaunayRefinement<P extends IPoint, CE, CF> extends GenDelaunayRefinement<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {
	public PDelaunayRefinement(
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final IPointConstructor<P> pointConstructor,
			boolean createHoles,
			final double minQuality,
			@NotNull final Function<IPoint, Double> circumRadiusFunc) {
		super(bound, constrains, () -> new PMesh<>((x,y) -> pointConstructor.create(x, y)), createHoles, minQuality, circumRadiusFunc);
	}
}
