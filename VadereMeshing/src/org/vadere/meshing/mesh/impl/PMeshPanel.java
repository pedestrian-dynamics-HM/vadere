package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.util.geometry.shapes.IPoint;

import java.awt.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class PMeshPanel extends MeshPanel<PVertex, PHalfEdge, PFace> {

	public PMeshPanel(
			@NotNull MeshRenderer<PVertex, PHalfEdge, PFace> meshRenderer,
			double width,
			double height) {
		super(meshRenderer, width, height);
	}

	public PMeshPanel(
			@NotNull final IMesh<PVertex, PHalfEdge, PFace> mesh,
			double width,
			double height) {
		super(mesh, width, height);
	}

	public PMeshPanel(
			@NotNull final IMesh<PVertex, PHalfEdge, PFace> mesh,
			double width,
			double height,
			@NotNull final Function<PFace, Color> colorFunction) {
		super(mesh, f -> false, width, height, colorFunction);
	}

	public PMeshPanel(
			@NotNull final IMesh<PVertex, PHalfEdge, PFace> mesh,
			double width,
			double height,
			@NotNull final Function<PFace, Color> faceColorFunction,
			@NotNull final Function<PHalfEdge, Color> edgeColorFunction) {
		super(mesh, f -> false, width, height, faceColorFunction, edgeColorFunction);
	}

	public PMeshPanel(
			@NotNull final IMesh<PVertex, PHalfEdge, PFace> mesh,
			double width,
			double height,
			@NotNull final Function<PFace, Color> faceColorFunction,
			@NotNull final Function<PHalfEdge, Color> edgeColorFunction,
			@NotNull final Function<PVertex, Color> vertexColorFunction) {
		super(mesh, f -> false, width, height, faceColorFunction, edgeColorFunction, vertexColorFunction);
	}

	public PMeshPanel(
			@NotNull final IMesh<PVertex, PHalfEdge, PFace> mesh,
			@NotNull Predicate<PFace> predicate,
			double width,
			double height) {
		super(mesh, predicate, width, height);
	}
}