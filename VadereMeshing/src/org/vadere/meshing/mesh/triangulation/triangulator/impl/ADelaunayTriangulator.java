package org.vadere.meshing.mesh.triangulation.triangulator.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.*;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenDelaunayTriangulator;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;

public class ADelaunayTriangulator extends GenDelaunayTriangulator<AVertex, AHalfEdge, AFace> {
	public ADelaunayTriangulator(@NotNull final Collection<? extends IPoint> pointSet) {
		super(AMeshWithDataStorage::constructEmpty, pointSet);
	}
}
