package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenVoronoiSegmentInsertion;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.Collection;
import java.util.function.Function;

public class PVoronoiSegmentInsertion<P extends IPoint, CE, CF> extends GenVoronoiSegmentInsertion<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {

	public PVoronoiSegmentInsertion(
			@NotNull final PSLG pslg,
			@NotNull final IPointConstructor<P> pointConstructor,
			boolean createHoles,
			@NotNull final Function<IPoint, Double> circumRadiusFunc) {
		super(pslg, () -> new PMesh<>((x, y) -> pointConstructor.create(x, y)), createHoles, circumRadiusFunc);
	}

	public PVoronoiSegmentInsertion(
			@NotNull final PSLG pslg,
			@NotNull final IPointConstructor<P> pointConstructor,
			@NotNull final Function<IPoint, Double> circumRadiusFunc) {
		super(pslg, () -> new PMesh<>((x, y) -> pointConstructor.create(x, y)), true, circumRadiusFunc);
	}
}
