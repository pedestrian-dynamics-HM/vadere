package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Predicate;

public class APMeshPanel<P extends IPoint> extends MeshPanel<P, Object, Object, PVertex<P, Object, Object>, PHalfEdge<P, Object, Object>, PFace<P, Object, Object>> {

	public APMeshPanel(@NotNull APMesh<P> mesh, double width, double height) {
		super(mesh, width, height);
	}

	public APMeshPanel(@NotNull APMesh<P> mesh, final Predicate<PFace<P, Object, Object>> alertPred, double width, double height) {
		super(mesh, alertPred, width, height);
	}
}
