package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.geometry.shapes.IPoint;

import java.awt.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class PMeshPanel<P extends IPoint, CE, CF> extends MeshPanel<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {

	public PMeshPanel(
			@NotNull MeshRenderer<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> meshRenderer,
			double width,
			double height) {
		super(meshRenderer, width, height);
	}

	public PMeshPanel(
			@NotNull final IMesh<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> mesh,
			double width,
			double height) {
		super(mesh, width, height);
	}

	public PMeshPanel(
			@NotNull final IMesh<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> mesh,
			double width,
			double height,
			@NotNull final Function<PFace<P, CE, CF>, Color> colorFunction) {
		super(mesh, f -> false, width, height, colorFunction);
	}

	public PMeshPanel(
			@NotNull final IMesh<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> mesh,
			@NotNull Predicate<PFace<P, CE, CF>> predicate,
			double width,
			double height) {
		super(mesh, predicate, width, height);
	}
}