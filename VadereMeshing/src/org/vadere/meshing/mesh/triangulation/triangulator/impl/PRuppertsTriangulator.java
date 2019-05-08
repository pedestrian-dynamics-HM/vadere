package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class PRuppertsTriangulator<P extends IPoint, CE, CF>  extends GenRuppertsTriangulator<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {

	public PRuppertsTriangulator(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			final double minAngle,
			@NotNull final IPointConstructor<P> pointConstructor) {
		super(() -> new PMesh<>(pointConstructor), pslg, minAngle, circumRadiusFunc, true);
	}

	public PRuppertsTriangulator(
			@NotNull final PSLG pslg,
			@NotNull final IPointConstructor<P> pointConstructor,
			final double minAngle) {
		super(() -> new PMesh<>(pointConstructor), pslg, minAngle, p -> Double.POSITIVE_INFINITY, true, true);
	}
}
