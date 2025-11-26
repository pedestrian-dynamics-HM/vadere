package org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.*;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AFace;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AVertex;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMeshBuilder;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;

import java.util.Collection;

public class AEikMesh extends GenEikMesh<AVertex, AHalfEdge, AFace> {

	public AEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound,
			@NotNull Collection<? extends VShape> obstacleShapes) {
		super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, obstacleShapes, ATriangleMeshBuilder::new);
	}

	public AEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound) {
		super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, ATriangleMeshBuilder::new);
	}

	public AEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound) {
		super(distanceFunc, e -> 1.0, initialEdgeLen, bound, ATriangleMeshBuilder::new);
	}
}