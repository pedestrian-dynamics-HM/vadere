package org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PTriangulation;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;

import java.util.Collection;
import java.util.Collections;

public class PEikMesh extends GenEikMesh<PVertex, PHalfEdge, PFace> {

	public PEikMesh(@NotNull final IEdgeLengthFunction edgeLengthFunc,
	                @NotNull final IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation) {
		super(edgeLengthFunc, triangulation);
	}

	public PEikMesh(@NotNull final IEdgeLengthFunction edgeLengthFunc,
	                @NotNull final IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation,
	                boolean refine) {
		super(edgeLengthFunc, triangulation, refine);
	}



	public PEikMesh(@NotNull final IDistanceFunction distanceFunc,
	                @NotNull final IEdgeLengthFunction edgeLengthFunc,
	                @NotNull final IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation,
	                final boolean refine) {
		super(distanceFunc, edgeLengthFunc, triangulation, refine);
	}

	public PEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			@NotNull PTriangulation triangulation,
			final boolean refine) {

		super(distanceFunc, edgeLengthFunc, triangulation, refine);
	}

	public PEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound,
			@NotNull Collection<? extends VShape> obstacleShapes) {

		super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, obstacleShapes, () -> new PMesh());
	}

	public PEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			@NotNull Collection<VPoint> fixPoints,
			double initialEdgeLen,
			@NotNull VRectangle bound
	) {
		super(distanceFunc, edgeLengthFunc, fixPoints, initialEdgeLen, bound, Collections.EMPTY_LIST,() -> new PMesh());
	}


	public PEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound) {

		super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound,
				() -> new PMesh());
	}

	public PEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound) {

		super(distanceFunc, e -> 1.0, initialEdgeLen, bound, () -> new PMesh());
	}

	public PEikMesh(
			double initialEdgeLen,
			@NotNull VRectangle bound) {

		super(p -> 1.0, e -> 1.0, initialEdgeLen, bound, () -> new PMesh());
	}

	public PEikMesh(
			@NotNull VPolygon polygon,
			double initialEdgeLen,
			@NotNull Collection<? extends VShape> obstacleShapes) {
		super(polygon, initialEdgeLen, obstacleShapes, () -> new PMesh());
	}
}