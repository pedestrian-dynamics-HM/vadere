package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.*;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles.PTriangleMeshBuilder;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Function;

public class PRuppertsTriangulator extends GenRuppertsTriangulator<PVertex, PHalfEdge, PFace> {

	public PRuppertsTriangulator(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			final double minAngle) {
		super(PTriangleMeshBuilder::new, pslg, minAngle, circumRadiusFunc, true);
	}

	public PRuppertsTriangulator(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			final double minAngle,
			final boolean createHoles) {
		super(PTriangleMeshBuilder::new, pslg, minAngle, circumRadiusFunc, createHoles);
	}

	public PRuppertsTriangulator(
			@NotNull final PSLG pslgBound,
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			final double minAngle,
			final boolean createHoles) {
		this(pslgBound, pslg, circumRadiusFunc, minAngle, createHoles, true);
	}

	public PRuppertsTriangulator(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			final double minAngle,
			final boolean createHoles,
			final boolean allowSegmentFaces) {
		super(PTriangleMeshBuilder::new, pslg, minAngle, circumRadiusFunc, createHoles, allowSegmentFaces);
	}

	public PRuppertsTriangulator(
			@NotNull final PSLG pslgBound,
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			final double minAngle,
			final boolean createHoles,
			final boolean allowSegmentFaces) {
		super(PTriangleMeshBuilder::new, pslgBound, pslg, minAngle, circumRadiusFunc, createHoles, allowSegmentFaces);
	}

	public PRuppertsTriangulator(
			@NotNull final PSLG pslg,
			final double minAngle) {
		super(PTriangleMeshBuilder::new, pslg, minAngle, p -> Double.POSITIVE_INFINITY, true, true);
	}
}
