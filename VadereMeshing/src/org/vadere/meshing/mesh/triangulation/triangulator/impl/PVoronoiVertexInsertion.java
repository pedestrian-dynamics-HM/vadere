package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.*;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenVoronoiVertexInsertion;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Function;

public class PVoronoiVertexInsertion extends GenVoronoiVertexInsertion<PVertex, PHalfEdge, PFace> {
	public PVoronoiVertexInsertion(
			@NotNull final PSLG pslg,
			boolean createHoles,
			@NotNull final Function<IPoint, Double> circumRadiusFunc) {
		super(pslg, PMeshWithDataStorage::constructEmpty, createHoles, circumRadiusFunc);
	}

	public PVoronoiVertexInsertion(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc) {
		super(pslg, PMeshWithDataStorage::constructEmpty, true, circumRadiusFunc);
	}
}
