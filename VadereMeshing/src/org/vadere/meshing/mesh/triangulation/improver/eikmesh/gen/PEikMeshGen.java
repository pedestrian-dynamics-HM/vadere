package org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;

import java.util.Collection;

public class PEikMeshGen<P extends EikMeshPoint, CE, CF> extends EikMesh<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {

	public PEikMeshGen(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound,
			@NotNull Collection<? extends VShape> obstacleShapes,
			@NotNull IPointConstructor<P> pointConstructor) {

		super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, obstacleShapes,
				() -> new PMesh<>((x, y) -> pointConstructor.create(x, y)));
	}

	public PEikMeshGen(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound,
			@NotNull IPointConstructor<P> pointConstructor) {

		super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound,
				() -> new PMesh<>((x, y) -> pointConstructor.create(x, y)));
	}

	public PEikMeshGen(
			@NotNull IDistanceFunction distanceFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound,
			@NotNull IPointConstructor<P> pointConstructor) {

		super(distanceFunc, e -> 1.0, initialEdgeLen, bound,
				() -> new PMesh<>((x, y) -> pointConstructor.create(x, y)));
	}

	public PEikMeshGen(
			@NotNull VPolygon polygon,
			double initialEdgeLen,
			@NotNull Collection<? extends VShape> obstacleShapes,
			@NotNull IPointConstructor<P> pointConstructor) {
		super(polygon, initialEdgeLen, obstacleShapes,
				() -> new PMesh<>((x, y) -> pointConstructor.create(x, y)));
	}
}
